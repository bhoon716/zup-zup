package bhoon.sugang_helper.common.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import bhoon.sugang_helper.campus.announcement.controller.AnnouncementController;
import bhoon.sugang_helper.campus.announcement.enums.AnnouncementSearchType;
import bhoon.sugang_helper.campus.announcement.service.AnnouncementService;
import bhoon.sugang_helper.seatalert.notification.controller.NotificationController;
import bhoon.sugang_helper.seatalert.notification.service.NotificationHistoryQueryService;
import bhoon.sugang_helper.support.feedback.controller.AdminFeedbackController;
import bhoon.sugang_helper.support.feedback.enums.AdminFeedbackDeletionFilter;
import bhoon.sugang_helper.support.feedback.service.FeedbackService;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.SliceImpl;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class FixedPageSizeContractTest {

    @Mock private AnnouncementService announcements;

    @Mock private NotificationHistoryQueryService notifications;

    @Mock private FeedbackService feedback;

    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.standaloneSetup(new AnnouncementController(announcements),
                        new NotificationController(notifications), new AdminFeedbackController(feedback))
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .setControllerAdvice(new GlobalExceptionHandler()).build();
    }

    @ParameterizedTest
    @DisplayName("클라이언트 크기로 고정 페이지 크기나 offset을 바꿀 수 없다")
    @ValueSource(strings = {"1", "100", "-1", "0", "invalid"})
    void clientSizeCannotChangePageSizeOrOffset(String size) throws Exception {
        // given
        when(announcements.getPublicAnnouncements(isNull(), any(), any()))
                .thenAnswer(invocation -> new PageImpl<>(List.of(), invocation.getArgument(2), 100));

        // when & then
        for (int page = 0; page < 3; page++) {
            mvc.perform(get("/api/v1/announcements").param("page", Integer.toString(page))
                            .param("size", size).param("sort", "createdAt,asc"))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.size").value(30))
                    .andExpect(jsonPath("$.number").value(page))
                    .andExpect(jsonPath("$.totalElements").value(100))
                    .andExpect(jsonPath("$.totalPages").value(4));
            verify(announcements).getPublicAnnouncements(null, AnnouncementSearchType.TITLE_CONTENT,
                    PageRequest.of(page, 30, Sort.by("createdAt").ascending()));
        }
    }

    @ParameterizedTest
    @DisplayName("알림과 필터된 관리자 피드백의 페이지 크기를 고정한다")
    @ValueSource(strings = {"1", "100", "-1"})
    void fixesNotificationAndFilteredAdminFeedbackSizes(String size) throws Exception {
        // given
        PageRequest expected = PageRequest.of(2, 30, Sort.by("createdAt").ascending());
        when(notifications.findForCurrentUser(expected)).thenReturn(new SliceImpl<>(List.of(), expected, true));
        when(feedback.getFeedbacksForAdmin(expected, AdminFeedbackDeletionFilter.DELETED))
                .thenReturn(new PageImpl<>(List.of(), expected, 100));

        // when & then
        for (String path : List.of("/api/v1/notifications/history", "/api/v1/admin/feedbacks")) {
            mvc.perform(get(path).param("size", size).param("page", "2").param("sort", "createdAt,asc")
                            .param("deletion", "DELETED"))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.size").value(30))
                    .andExpect(jsonPath("$.number").value(2));
        }
        verify(notifications).findForCurrentUser(expected);
        verify(feedback).getFeedbacksForAdmin(expected, AdminFeedbackDeletionFilter.DELETED);
    }
}
