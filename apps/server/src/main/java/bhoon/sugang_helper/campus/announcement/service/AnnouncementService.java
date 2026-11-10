package bhoon.sugang_helper.campus.announcement.service;

import bhoon.sugang_helper.campus.announcement.AnnouncementPreviewText;
import bhoon.sugang_helper.campus.announcement.dto.request.AnnouncementRequest;
import bhoon.sugang_helper.campus.announcement.dto.response.AnnouncementDetailResponse;
import bhoon.sugang_helper.campus.announcement.dto.response.AnnouncementListResponse;
import bhoon.sugang_helper.campus.announcement.entity.Announcement;
import bhoon.sugang_helper.campus.announcement.enums.AnnouncementSearchType;
import bhoon.sugang_helper.campus.announcement.repository.AnnouncementRepository;
import bhoon.sugang_helper.campus.api.AnnouncementPreview;
import bhoon.sugang_helper.campus.api.Announcements;
import bhoon.sugang_helper.common.api.error.CustomException;
import bhoon.sugang_helper.common.api.error.ErrorCode;
import bhoon.sugang_helper.common.api.web.PageableGuard;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AnnouncementService implements Announcements {

    private static final int MAX_KEYWORD_LENGTH = 100;
    private static final int MAX_PAGE_SIZE = 100;

    private final AnnouncementRepository announcementRepository;
    private final MeterRegistry meterRegistry;

    public List<AnnouncementListResponse> getPublicAnnouncements(String keyword, AnnouncementSearchType searchType) {
        return getPublicAnnouncements(keyword, searchType, PageRequest.of(0, MAX_PAGE_SIZE)).getContent();
    }

    public Page<AnnouncementListResponse> getPublicAnnouncements(
            String keyword, AnnouncementSearchType searchType, Pageable pageable) {
        return queryPublicAnnouncements(keyword, searchType, pageable, AnnouncementListResponse::from);
    }

    public List<AnnouncementPreview> findDashboardAnnouncements(int limit) {
        return queryPublicAnnouncements(null, AnnouncementSearchType.TITLE_CONTENT, PageRequest.of(0, limit),
                AnnouncementService::toPreview).getContent();
    }

    private <T> Page<T> queryPublicAnnouncements(
            String keyword, AnnouncementSearchType searchType, Pageable pageable, Function<Announcement, T> mapper) {
        Pageable bounded = PageableGuard.requireBounded(pageable, MAX_PAGE_SIZE, PageableGuard.DEFAULT_MAX_OFFSET);
        String normalizedKeyword = normalizeKeyword(keyword);
        AnnouncementSearchType effectiveSearchType = Objects.requireNonNullElse(
                searchType, AnnouncementSearchType.TITLE_CONTENT);
        long startedAtNanos = System.nanoTime();
        try {
            return findPublicAnnouncements(normalizedKeyword, effectiveSearchType, bounded).map(mapper);
        } finally {
            recordSearchLatency(effectiveSearchType, !normalizedKeyword.isEmpty(), startedAtNanos);
        }
    }

    private static AnnouncementPreview toPreview(Announcement announcement) {
        return new AnnouncementPreview(announcement.getId(), announcement.getTitle(),
                AnnouncementPreviewText.from(announcement.getContent()), announcement.isPinned(),
                announcement.getCreatedAt());
    }

    public AnnouncementDetailResponse getPublicAnnouncement(Long id) {
        Announcement announcement = announcementRepository.findByIdAndPublishedTrue(id)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND, "공개된 공지사항을 찾을 수 없습니다."));
        return AnnouncementDetailResponse.from(announcement);
    }

    public List<AnnouncementDetailResponse> getAdminAnnouncements() {
        return getAdminAnnouncements(PageRequest.of(0, MAX_PAGE_SIZE)).getContent();
    }

    public Page<AnnouncementDetailResponse> getAdminAnnouncements(Pageable pageable) {
        return announcementRepository.findAllByOrderByPinnedDescCreatedAtDesc(
                        PageableGuard.requireBounded(pageable, MAX_PAGE_SIZE, PageableGuard.DEFAULT_MAX_OFFSET))
                .map(AnnouncementDetailResponse::from);
    }

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

    @Transactional
    public void deleteAnnouncement(Long id) {
        Announcement announcement = getAnnouncement(id);
        announcementRepository.delete(announcement);
    }

    private Announcement getAnnouncement(Long id) {
        return announcementRepository.findById(id)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND, "공지사항을 찾을 수 없습니다."));
    }

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

    private void recordSearchLatency(AnnouncementSearchType searchType, boolean hasKeyword, long startedAtNanos) {
        if (meterRegistry == null) {
            return;
        }
        meterRegistry.timer("announcement.search.latency", "searchType", searchType.name(),
                        "hasKeyword", Boolean.toString(hasKeyword))
                .record(System.nanoTime() - startedAtNanos, TimeUnit.NANOSECONDS);
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
