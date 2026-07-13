package bhoon.sugang_helper.announcement.presentation;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import bhoon.sugang_helper.announcement.application.AnnouncementListResponse;
import bhoon.sugang_helper.announcement.application.AnnouncementSearchType;
import bhoon.sugang_helper.announcement.application.AnnouncementService;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class AnnouncementControllerTest {

    @Mock
    private AnnouncementService announcementService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new AnnouncementController(announcementService))
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .build();
    }

    @Test
    void returnsPagedAnnouncementsWithoutChangingPinnedOrderingContract() throws Exception {
        AnnouncementListResponse response = AnnouncementListResponse.builder()
                .id(1L)
                .title("고정 공지")
                .previewContent("미리보기")
                .pinned(true)
                .createdAt(LocalDateTime.of(2026, 7, 13, 10, 0))
                .build();
        when(announcementService.getPublicAnnouncements(
                eq("검색"), eq(AnnouncementSearchType.TITLE_CONTENT), eq(PageRequest.of(1, 2))))
                .thenReturn(new PageImpl<>(List.of(response), PageRequest.of(1, 2), 3));

        mockMvc.perform(get("/api/v1/announcements")
                        .param("keyword", "검색")
                        .param("page", "1")
                        .param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].title").value("고정 공지"))
                .andExpect(jsonPath("$.data.content[0].pinned").value(true))
                .andExpect(jsonPath("$.data.number").value(1))
                .andExpect(jsonPath("$.data.size").value(2));
    }
}
