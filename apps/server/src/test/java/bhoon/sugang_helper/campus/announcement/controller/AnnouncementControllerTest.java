package bhoon.sugang_helper.campus.announcement.controller;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import bhoon.sugang_helper.campus.announcement.dto.response.AnnouncementListResponse;
import bhoon.sugang_helper.campus.announcement.entity.Announcement;
import bhoon.sugang_helper.campus.announcement.enums.AnnouncementSearchType;
import bhoon.sugang_helper.campus.announcement.service.AnnouncementService;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.test.util.ReflectionTestUtils;
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
    @DisplayName("고정 여부를 포함한 공지 페이지와 고정 페이지 크기를 반환한다")
    void returnsPagedAnnouncementsWithoutChangingPinnedOrderingContract() throws Exception {
        // given
        Announcement announcement = new Announcement("고정 공지", "미리보기", true, true);
        ReflectionTestUtils.setField(announcement, "id", 1L);
        ReflectionTestUtils.setField(announcement, "createdAt", LocalDateTime.of(2026, 7, 13, 10, 0));
        AnnouncementListResponse response = AnnouncementListResponse.from(announcement);
        when(announcementService.getPublicAnnouncements(
                eq("검색"), eq(AnnouncementSearchType.TITLE_CONTENT), eq(PageRequest.of(1, 10))))
                .thenReturn(new PageImpl<>(List.of(response), PageRequest.of(1, 10), 21));

        // when & then
        mockMvc.perform(get("/api/v1/announcements")
                        .param("keyword", "검색")
                        .param("page", "1")
                        .param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].title").value("고정 공지"))
                .andExpect(jsonPath("$.content[0].pinned").value(true))
                .andExpect(jsonPath("$.number").value(1))
                .andExpect(jsonPath("$.size").value(10));
    }
}
