package bhoon.sugang_helper.announcement.application;

import bhoon.sugang_helper.announcement.domain.Announcement;
import bhoon.sugang_helper.announcement.domain.AnnouncementRepository;
import bhoon.sugang_helper.common.error.CustomException;
import bhoon.sugang_helper.common.error.ErrorCode;
import bhoon.sugang_helper.common.web.PageableGuard;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AnnouncementService {

    private static final int MAX_KEYWORD_LENGTH = 100;
    private static final int MAX_PAGE_SIZE = 100;

    private final AnnouncementRepository announcementRepository;
    private final MeterRegistry meterRegistry;

    /**
     * 공개된 공지사항 목록을 검색 조건에 따라 조회합니다.
     */
    public List<AnnouncementListResponse> getPublicAnnouncements(String keyword, AnnouncementSearchType searchType) {
        return getPublicAnnouncements(keyword, searchType, PageRequest.of(0, MAX_PAGE_SIZE)).getContent();
    }

    public Page<AnnouncementListResponse> getPublicAnnouncements(
            String keyword, AnnouncementSearchType searchType, Pageable pageable) {
        Pageable bounded = PageableGuard.requireBounded(pageable, MAX_PAGE_SIZE, 10_000);
        String normalizedKeyword = normalizeKeyword(keyword);
        AnnouncementSearchType effectiveSearchType = searchType == null
                ? AnnouncementSearchType.TITLE_CONTENT : searchType;
        long started = System.nanoTime();
        try {
            return findPublicAnnouncements(normalizedKeyword, effectiveSearchType, bounded)
                    .map(AnnouncementListResponse::from);
        } finally {
            if (meterRegistry != null) {
                meterRegistry.timer("announcement.search.latency", "searchType", effectiveSearchType.name(),
                        "hasKeyword", Boolean.toString(!normalizedKeyword.isEmpty()))
                        .record(System.nanoTime() - started, java.util.concurrent.TimeUnit.NANOSECONDS);
            }
        }
    }

    /**
     * 공개된 특정 공지사항의 상세 내용을 조회합니다.
     */
    public AnnouncementDetailResponse getPublicAnnouncement(Long id) {
        Announcement announcement = announcementRepository.findByIdAndPublishedTrue(id)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND, "공개된 공지사항을 찾을 수 없습니다."));
        return AnnouncementDetailResponse.from(announcement);
    }

    /**
     * 관리자용 전체 공지사항 목록을 조회합니다. (비공개 포함)
     */
    public List<AnnouncementDetailResponse> getAdminAnnouncements() {
        return getAdminAnnouncements(PageRequest.of(0, MAX_PAGE_SIZE)).getContent();
    }

    public Page<AnnouncementDetailResponse> getAdminAnnouncements(Pageable pageable) {
        return announcementRepository.findAllByOrderByPinnedDescCreatedAtDesc(
                        PageableGuard.requireBounded(pageable, MAX_PAGE_SIZE, 10_000))
                .map(AnnouncementDetailResponse::from);
    }

    /**
     * 새로운 공지사항을 생성하고 저장합니다.
     */
    @Transactional
    public AnnouncementDetailResponse createAnnouncement(AnnouncementRequest request) {
        Announcement announcement = Announcement.builder()
                .title(request.getTitle().trim())
                .content(request.getContent().trim())
                .pinned(request.isPinnedOrDefault())
                .published(request.isPublishedOrDefault())
                .build();
        return AnnouncementDetailResponse.from(announcementRepository.save(announcement));
    }

    /**
     * 기존 공지사항 정보를 수정합니다.
     */
    @Transactional
    public AnnouncementDetailResponse updateAnnouncement(Long id, AnnouncementRequest request) {
        Announcement announcement = getAnnouncement(id);
        announcement.update(
                request.getTitle().trim(),
                request.getContent().trim(),
                request.isPinnedOrDefault(),
                request.isPublishedOrDefault());
        return AnnouncementDetailResponse.from(announcement);
    }

    /**
     * 특정 공지사항을 영구적으로 삭제합니다.
     */
    @Transactional
    public void deleteAnnouncement(Long id) {
        Announcement announcement = getAnnouncement(id);
        announcementRepository.delete(announcement);
    }

    /**
     * ID를 기반으로 공지사항 엔티티를 조회하거나 예외를 발생시킵니다.
     */
    private Announcement getAnnouncement(Long id) {
        return announcementRepository.findById(id)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND, "공지사항을 찾을 수 없습니다."));
    }

    /**
     * 검색 조건에 따른 공개 공지사항 필터링 로직을 수행합니다.
     */
    private Page<Announcement> findPublicAnnouncements(
            String keyword, AnnouncementSearchType searchType, Pageable pageable) {
        if (keyword.isEmpty()) {
            return announcementRepository.findByPublishedTrueOrderByPinnedDescCreatedAtDesc(pageable);
        }
        return switch (searchType) {
            case TITLE -> announcementRepository.searchPublishedByTitle(keyword, pageable);
            case CONTENT -> announcementRepository.searchPublishedByContent(keyword, pageable);
            case TITLE_CONTENT -> announcementRepository.searchPublishedByTitleOrContent(keyword, pageable);
        };
    }

    private String normalizeKeyword(String keyword) {
        String normalized = keyword == null ? "" : keyword.trim();
        if (normalized.length() > MAX_KEYWORD_LENGTH) {
            throw new CustomException(ErrorCode.INVALID_INPUT,
                    "검색어는 " + MAX_KEYWORD_LENGTH + "자 이하로 입력해주세요.");
        }
        return normalized;
    }
}
