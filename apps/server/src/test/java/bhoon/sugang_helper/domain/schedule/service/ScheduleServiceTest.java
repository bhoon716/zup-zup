package bhoon.sugang_helper.domain.schedule.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import bhoon.sugang_helper.common.error.CustomException;
import bhoon.sugang_helper.common.error.ErrorCode;
import bhoon.sugang_helper.domain.schedule.entity.Schedule;
import bhoon.sugang_helper.domain.schedule.repository.ScheduleRepository;
import bhoon.sugang_helper.domain.schedule.request.ScheduleRequest;
import bhoon.sugang_helper.domain.schedule.response.ScheduleResponse;
import java.time.LocalDate;
import java.time.LocalTime;
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
class ScheduleServiceTest {

    @InjectMocks
    private ScheduleService scheduleService;

    @Mock
    private ScheduleRepository scheduleRepository;

    @Test
    @DisplayName("진행 중이거나 예정된 일정 목록을 조회한다")
    void getUpcomingSchedules() {
        // given
        LocalDate today = LocalDate.now();
        Schedule schedule = Schedule.builder()
                .title("수강신청 장바구니")
                .scheduleDate(today.plusDays(3))
                .scheduleTime(LocalTime.of(10, 0))
                .build();
        ReflectionTestUtils.setField(schedule, "id", 1L);

        when(scheduleRepository.findUpcomingSchedules(today)).thenReturn(List.of(schedule));

        // when
        List<ScheduleResponse> responses = scheduleService.getUpcomingSchedules();

        // then
        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).getTitle()).isEqualTo("수강신청 장바구니");
        assertThat(responses.get(0).getDDay()).isEqualTo("D-3");
    }

    @Test
    @DisplayName("새로운 일정을 생성한다")
    void createSchedule() {
        // given
        ScheduleRequest request = new ScheduleRequest("본 수강신청", LocalDate.now().plusDays(5), LocalTime.of(8, 0));
        Schedule schedule = Schedule.builder()
                .title(request.getTitle())
                .scheduleDate(request.getScheduleDate())
                .scheduleTime(request.getScheduleTime())
                .build();
        ReflectionTestUtils.setField(schedule, "id", 1L);

        when(scheduleRepository.save(any(Schedule.class))).thenReturn(schedule);

        // when
        ScheduleResponse response = scheduleService.createSchedule(request);

        // then
        assertThat(response.getTitle()).isEqualTo("본 수강신청");
        assertThat(response.getDDay()).isEqualTo("D-5");
        verify(scheduleRepository).save(any(Schedule.class));
    }

    @Test
    @DisplayName("기존 일정을 수정한다")
    void updateSchedule() {
        // given
        Schedule schedule = Schedule.builder()
                .title("기존 일정")
                .scheduleDate(LocalDate.now().plusDays(1))
                .build();
        ReflectionTestUtils.setField(schedule, "id", 1L);

        ScheduleRequest updateRequest = new ScheduleRequest("수정된 일정", LocalDate.now().plusDays(2), LocalTime.of(12, 0));

        when(scheduleRepository.findById(1L)).thenReturn(Optional.of(schedule));

        // when
        ScheduleResponse response = scheduleService.updateSchedule(1L, updateRequest);

        // then
        assertThat(response.getTitle()).isEqualTo("수정된 일정");
        assertThat(schedule.getScheduleTime()).isEqualTo(LocalTime.of(12, 0));
    }

    @Test
    @DisplayName("일정을 찾을 수 없으면 예외가 발생한다")
    void updateScheduleNotFound() {
        // given
        ScheduleRequest request = new ScheduleRequest("테스트", LocalDate.now(), null);
        when(scheduleRepository.findById(1L)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> scheduleService.updateSchedule(1L, request))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.NOT_FOUND);
    }
}
