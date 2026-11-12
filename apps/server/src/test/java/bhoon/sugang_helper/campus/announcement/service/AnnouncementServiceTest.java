package bhoon.sugang_helper.campus.announcement.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

import bhoon.sugang_helper.campus.announcement.dto.response.AnnouncementDetailResponse;
import bhoon.sugang_helper.campus.announcement.dto.response.AnnouncementListResponse;
import bhoon.sugang_helper.campus.announcement.entity.Announcement;
import bhoon.sugang_helper.campus.announcement.repository.AnnouncementRepository;
import bhoon.sugang_helper.campus.api.AnnouncementPreview;
import bhoon.sugang_helper.common.api.error.CustomException;
import bhoon.sugang_helper.common.api.error.ErrorCode;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class AnnouncementServiceTest {

    @Mock
    private AnnouncementRepository announcementRepository;

    @InjectMocks
    private AnnouncementService announcementService;

    @Test
    @DisplayName("공개된 공지사항 목록 조회 성공")
    void getPublicAnnouncements_success() {
        // given
        Announcement announcement = createAnnouncement("제목", "내용", true, true);
        given(announcementRepository.findByPublishedTrueAndDeletedAtIsNullOrderByPinnedDescCreatedAtDesc(any()))
                .willReturn(new PageImpl<>(List.of(announcement)));

        // when
        List<AnnouncementListResponse> result = announcementService.getPublicAnnouncements(null, null);

        // then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTitle()).isEqualTo("제목");
    }

    @Test
    @DisplayName("대시보드 공지는 공개 목록 순서와 미리보기 형식 및 조회 지표를 유지한다")
    void dashboardAnnouncementsPreservePreviewsAndMetric() {
        // given
        String longContent = "가".repeat(121);
        Announcement pinned = createAnnouncement("고정 공지", "**강조**\r\n" + longContent, true, true);
        Announcement recent = createAnnouncement("최근 공지", "[신청 안내](https://example.org)", false, true);
        PageRequest firstPage = PageRequest.of(0, 2);
        given(announcementRepository.findByPublishedTrueAndDeletedAtIsNullOrderByPinnedDescCreatedAtDesc(firstPage))
                .willReturn(new PageImpl<>(List.of(pinned, recent)));

        SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
        AnnouncementService service = new AnnouncementService(announcementRepository, meterRegistry, null);

        // when
        List<AnnouncementPreview> previews = service.findDashboardAnnouncements(2);

        // then
        assertThat(previews).containsExactly(
                new AnnouncementPreview(null, pinned.getTitle(), "강조 " + "가".repeat(117) + "...", true, null),
                new AnnouncementPreview(null, recent.getTitle(), "신청 안내", false, null));
        assertThat(meterRegistry.get("announcement.search.latency")
                .tags("searchType", "TITLE_CONTENT", "hasKeyword", "false")
                .timer().count()).isEqualTo(1);
    }

    @Test
    @DisplayName("공지사항 검색어 길이 제한")
    void getPublicAnnouncements_keywordTooLong() {
        // given
        String keyword = "a".repeat(101);

        // when & then
        assertThatThrownBy(() -> announcementService.getPublicAnnouncements(keyword, null, PageRequest.of(0, 20)))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_INPUT);
    }

    @Test
    @DisplayName("공지사항 상세 조회 성공")
    void getPublicAnnouncement_success() {
        // given
        Long id = 1L;
        Announcement announcement = createAnnouncement("제목", "내용", false, true);
        ReflectionTestUtils.setField(announcement, "id", id);
        given(announcementRepository.findByIdAndPublishedTrueAndDeletedAtIsNull(id)).willReturn(Optional.of(announcement));

        // when
        AnnouncementDetailResponse result = announcementService.getPublicAnnouncement(id);

        // then
        assertThat(result.getTitle()).isEqualTo("제목");
    }

    @Test
    @DisplayName("공지사항 상세 조회 실패 - 존재하지 않거나 비공개")
    void getPublicAnnouncement_notFound() {
        // given
        Long id = 1L;
        given(announcementRepository.findByIdAndPublishedTrueAndDeletedAtIsNull(id)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> announcementService.getPublicAnnouncement(id))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.NOT_FOUND);
    }

    private Announcement createAnnouncement(String title, String content, boolean pinned, boolean published) {
        return Announcement.builder()
                .title(title)
                .content(content)
                .pinned(pinned)
                .published(published)
                .build();
    }
}
