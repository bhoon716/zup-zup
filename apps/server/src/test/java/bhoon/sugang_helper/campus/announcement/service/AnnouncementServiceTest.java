package bhoon.sugang_helper.campus.announcement.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import bhoon.sugang_helper.campus.announcement.dto.request.AnnouncementRequest;
import bhoon.sugang_helper.campus.announcement.dto.response.AnnouncementDetailResponse;
import bhoon.sugang_helper.campus.announcement.dto.response.AnnouncementListResponse;
import bhoon.sugang_helper.campus.announcement.entity.Announcement;
import bhoon.sugang_helper.campus.announcement.repository.AnnouncementRepository;
import bhoon.sugang_helper.common.error.CustomException;
import bhoon.sugang_helper.common.error.ErrorCode;
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

        Announcement announcement = createAnnouncement("제목", "내용", true, true);
        given(announcementRepository.findByPublishedTrueOrderByPinnedDescCreatedAtDesc(any()))
                .willReturn(new PageImpl<>(List.of(announcement)));


        List<AnnouncementListResponse> result = announcementService.getPublicAnnouncements(null, null);


        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTitle()).isEqualTo("제목");
    }

    @Test
    @DisplayName("공지사항 검색어 길이 제한")
    void getPublicAnnouncements_keywordTooLong() {
        String keyword = "a".repeat(101);

        assertThatThrownBy(() -> announcementService.getPublicAnnouncements(keyword, null, PageRequest.of(0, 20)))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_INPUT);
    }

    @Test
    @DisplayName("공지사항 상세 조회 성공")
    void getPublicAnnouncement_success() {

        Long id = 1L;
        Announcement announcement = createAnnouncement("제목", "내용", false, true);
        ReflectionTestUtils.setField(announcement, "id", id);
        given(announcementRepository.findByIdAndPublishedTrue(id)).willReturn(Optional.of(announcement));


        AnnouncementDetailResponse result = announcementService.getPublicAnnouncement(id);


        assertThat(result.getTitle()).isEqualTo("제목");
    }

    @Test
    @DisplayName("공지사항 상세 조회 실패 - 존재하지 않거나 비공개")
    void getPublicAnnouncement_notFound() {

        Long id = 1L;
        given(announcementRepository.findByIdAndPublishedTrue(id)).willReturn(Optional.empty());


        assertThatThrownBy(() -> announcementService.getPublicAnnouncement(id))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.NOT_FOUND);
    }

    @Test
    @DisplayName("공지사항 생성 성공")
    void createAnnouncement_success() {

        AnnouncementRequest request = createRequest("제목 ", " 내용", true, true);
        Announcement savedAnnouncement = createAnnouncement("제목", "내용", true, true);
        given(announcementRepository.save(any(Announcement.class))).willReturn(savedAnnouncement);


        AnnouncementDetailResponse result = announcementService.createAnnouncement(request);


        assertThat(result.getTitle()).isEqualTo("제목");
        verify(announcementRepository).save(any(Announcement.class));
    }

    @Test
    @DisplayName("공지사항 삭제 성공")
    void deleteAnnouncement_success() {

        Long id = 1L;
        Announcement announcement = createAnnouncement("제목", "내용", false, true);
        given(announcementRepository.findById(id)).willReturn(Optional.of(announcement));


        announcementService.deleteAnnouncement(id);


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
