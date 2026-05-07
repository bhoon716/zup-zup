package bhoon.sugang_helper.domain.announcement.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import bhoon.sugang_helper.common.error.CustomException;
import bhoon.sugang_helper.common.error.ErrorCode;
import bhoon.sugang_helper.domain.announcement.entity.Announcement;
import bhoon.sugang_helper.domain.announcement.repository.AnnouncementRepository;
import bhoon.sugang_helper.domain.announcement.request.AnnouncementRequest;
import bhoon.sugang_helper.domain.announcement.request.AnnouncementSearchType;
import bhoon.sugang_helper.domain.announcement.response.AnnouncementDetailResponse;
import bhoon.sugang_helper.domain.announcement.response.AnnouncementListResponse;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
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
        given(announcementRepository.findByPublishedTrueOrderByPinnedDescCreatedAtDesc())
                .willReturn(List.of(announcement));

        // when
        List<AnnouncementListResponse> result = announcementService.getPublicAnnouncements(null, null);

        // then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTitle()).isEqualTo("제목");
    }

    @Test
    @DisplayName("공지사항 상세 조회 성공")
    void getPublicAnnouncement_success() {
        // given
        Long id = 1L;
        Announcement announcement = createAnnouncement("제목", "내용", false, true);
        ReflectionTestUtils.setField(announcement, "id", id);
        given(announcementRepository.findByIdAndPublishedTrue(id)).willReturn(Optional.of(announcement));

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
        given(announcementRepository.findByIdAndPublishedTrue(id)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> announcementService.getPublicAnnouncement(id))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.NOT_FOUND);
    }

    @Test
    @DisplayName("공지사항 생성 성공")
    void createAnnouncement_success() {
        // given
        AnnouncementRequest request = createRequest("제목 ", " 내용", true, true);
        Announcement savedAnnouncement = createAnnouncement("제목", "내용", true, true);
        given(announcementRepository.save(any(Announcement.class))).willReturn(savedAnnouncement);

        // when
        AnnouncementDetailResponse result = announcementService.createAnnouncement(request);

        // then
        assertThat(result.getTitle()).isEqualTo("제목");
        verify(announcementRepository).save(any(Announcement.class));
    }

    @Test
    @DisplayName("공지사항 삭제 성공")
    void deleteAnnouncement_success() {
        // given
        Long id = 1L;
        Announcement announcement = createAnnouncement("제목", "내용", false, true);
        given(announcementRepository.findById(id)).willReturn(Optional.of(announcement));

        // when
        announcementService.deleteAnnouncement(id);

        // then
        verify(announcementRepository).delete(announcement);
    }

    private Announcement createAnnouncement(String title, String content, boolean pinned, boolean published) {
        return Announcement.builder()
                .title(title)
                .content(content)
                .pinned(pinned)
                .published(published)
                .build();
    }

    private AnnouncementRequest createRequest(String title, String content, boolean pinned, boolean published) {
        AnnouncementRequest request = new AnnouncementRequest();
        ReflectionTestUtils.setField(request, "title", title);
        ReflectionTestUtils.setField(request, "content", content);
        ReflectionTestUtils.setField(request, "pinned", pinned);
        ReflectionTestUtils.setField(request, "published", published);
        return request;
    }
}
