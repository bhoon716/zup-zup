package bhoon.sugang_helper.domain.announcement.service;

import bhoon.sugang_helper.common.error.CustomException;
import bhoon.sugang_helper.common.error.ErrorCode;
import bhoon.sugang_helper.common.util.SecurityUtil;
import bhoon.sugang_helper.domain.announcement.entity.Announcement;
import bhoon.sugang_helper.domain.announcement.repository.AnnouncementRepository;
import bhoon.sugang_helper.domain.announcement.request.AnnouncementRequest;
import bhoon.sugang_helper.domain.announcement.request.AnnouncementSearchType;
import bhoon.sugang_helper.domain.announcement.response.AnnouncementDetailResponse;
import bhoon.sugang_helper.domain.announcement.response.AnnouncementListResponse;
import bhoon.sugang_helper.domain.user.entity.User;
import bhoon.sugang_helper.domain.user.repository.UserRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AnnouncementService {

    private final AnnouncementRepository announcementRepository;
    private final UserRepository userRepository;

    /**
     * 공개된 공지사항 목록을 검색 조건에 따라 조회합니다.
     */
    public List<AnnouncementListResponse> getPublicAnnouncements(String keyword, AnnouncementSearchType searchType) {
        return findPublicAnnouncements(keyword, searchType)
                .stream()
                .map(AnnouncementListResponse::from)
                .toList();
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
        return announcementRepository.findAllByOrderByPinnedDescCreatedAtDesc()
                .stream()
                .map(AnnouncementDetailResponse::from)
                .toList();
    }

    /**
     * 새로운 공지사항을 생성하고 저장합니다.
     */
    @Transactional
    public AnnouncementDetailResponse createAnnouncement(AnnouncementRequest request) {
        User user = getCurrentUser();
        Announcement announcement = Announcement.builder()
                .title(request.getTitle().trim())
                .content(request.getContent().trim())
                .pinned(request.isPinnedOrDefault())
                .published(request.isPublishedOrDefault())
                .authorName(user.getName())
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
    private List<Announcement> findPublicAnnouncements(String keyword, AnnouncementSearchType searchType) {
        if (keyword == null || keyword.isBlank()) {
            return announcementRepository.findByPublishedTrueOrderByPinnedDescCreatedAtDesc();
        }

        String normalizedKeyword = keyword.trim();
        AnnouncementSearchType effectiveSearchType = searchType == null ? AnnouncementSearchType.TITLE_CONTENT
                : searchType;

        return switch (effectiveSearchType) {
            case TITLE -> announcementRepository.searchPublishedByTitle(normalizedKeyword);
            case CONTENT -> announcementRepository.searchPublishedByContent(normalizedKeyword);
            case TITLE_CONTENT -> announcementRepository.searchPublishedByTitleOrContent(normalizedKeyword);
        };
    }

    /**
     * 현재 인증된 사용자 정보를 조회합니다.
     */
    private User getCurrentUser() {
        String email = SecurityUtil.getCurrentUserEmail();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND, "email: " + email));
    }
}
