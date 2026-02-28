package bhoon.sugang_helper.domain.admin.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import bhoon.sugang_helper.domain.schedule.request.ScheduleRequest;
import bhoon.sugang_helper.domain.schedule.response.ScheduleResponse;
import bhoon.sugang_helper.domain.schedule.service.ScheduleService;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = AdminScheduleController.class, excludeFilters = {
                @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = bhoon.sugang_helper.common.config.SecurityConfig.class)
})
@AutoConfigureMockMvc(addFilters = false) // 시큐리티 필터 무시
class AdminScheduleControllerTest {

        @Autowired
        private MockMvc mockMvc;

        @Autowired
        private ObjectMapper objectMapper;

        @MockitoBean
        private ScheduleService scheduleService;

        @MockitoBean
        private bhoon.sugang_helper.common.security.jwt.JwtProvider jwtProvider;

        @MockitoBean
        private org.springframework.data.jpa.mapping.JpaMetamodelMappingContext jpaMetamodelMappingContext;

        @Test
        @DisplayName("모든 일정 목록을 조회한다")
        void getAllSchedules() throws Exception {
                // given
                List<ScheduleResponse> mockResponses = List.of(
                                ScheduleResponse.builder()
                                                .id(1L)
                                                .title("어드민 전체 테스트")
                                                .scheduleDate(LocalDate.now())
                                                .dDay("D-Day")
                                                .build());
                given(scheduleService.getAllSchedules()).willReturn(mockResponses);

                // when & then
                mockMvc.perform(get("/api/v1/admin/schedules"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$[0].title").value("어드민 전체 테스트"));
        }

        @Test
        @DisplayName("신규 일정을 생성한다")
        void createSchedule() throws Exception {
                // given
                ScheduleRequest request = new ScheduleRequest("일정", LocalDate.now(), LocalTime.of(9, 0));
                ScheduleResponse response = ScheduleResponse.builder()
                                .id(1L)
                                .title("일정")
                                .scheduleDate(LocalDate.now())
                                .dDay("D-Day")
                                .build();
                given(scheduleService.createSchedule(any())).willReturn(response);

                // when & then
                mockMvc.perform(post("/api/v1/admin/schedules")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isCreated())
                                .andExpect(jsonPath("$.id").value(1L));
        }

        @Test
        @DisplayName("기존 일정을 수정한다")
        void updateSchedule() throws Exception {
                // given
                ScheduleRequest request = new ScheduleRequest("수정됨", LocalDate.now(), LocalTime.of(10, 0));
                ScheduleResponse response = ScheduleResponse.builder()
                                .id(1L)
                                .title("수정됨")
                                .scheduleDate(LocalDate.now())
                                .dDay("D-Day")
                                .build();
                given(scheduleService.updateSchedule(eq(1L), any())).willReturn(response);

                // when & then
                mockMvc.perform(put("/api/v1/admin/schedules/1")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.title").value("수정됨"));
        }

        @Test
        @DisplayName("일정을 삭제한다")
        void deleteSchedule() throws Exception {
                // given & when & then
                mockMvc.perform(delete("/api/v1/admin/schedules/1"))
                                .andExpect(status().isNoContent());
        }
}
