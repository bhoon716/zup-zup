package bhoon.sugang_helper.domain.schedule.controller;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import bhoon.sugang_helper.domain.schedule.response.ScheduleResponse;
import bhoon.sugang_helper.domain.schedule.service.ScheduleService;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import bhoon.sugang_helper.common.config.SecurityConfig;
import bhoon.sugang_helper.common.security.jwt.JwtProvider;

@WebMvcTest(controllers = ScheduleController.class, excludeFilters = {
        @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = SecurityConfig.class)
})
@AutoConfigureMockMvc(addFilters = false) // 시큐리티 필터 비활성화 (순수 컨트롤러 로직만 테스트)
class ScheduleControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ScheduleService scheduleService;

    @MockitoBean
    private JwtProvider jwtProvider;

    @MockitoBean
    private JpaMetamodelMappingContext jpaMetamodelMappingContext;

    @Test
    @DisplayName("예정된 주요 일정 목록을 조회한다")
    void getUpcomingSchedules() throws Exception {
        // given
        List<ScheduleResponse> mockResponses = List.of(
                ScheduleResponse.builder()
                        .id(1L)
                        .scheduleType("테스트 일정")
                        .startDate(LocalDate.now().plusDays(1))
                        .endDate(LocalDate.now().plusDays(1))
                        .startTime(LocalTime.of(10, 0))
                        .endTime(LocalTime.of(11, 0))
                        .dDay("D-1")
                        .build());
        given(scheduleService.getUpcomingSchedules()).willReturn(mockResponses);

        // when & then
        mockMvc.perform(get("/api/v1/schedules"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].scheduleType").value("테스트 일정"))
                .andExpect(jsonPath("$[0].dDay").value("D-1"))
                .andExpect(jsonPath("$[0].id").value(1L));
    }
}
