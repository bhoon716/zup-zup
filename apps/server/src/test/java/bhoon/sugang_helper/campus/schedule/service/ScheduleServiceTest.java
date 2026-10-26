package bhoon.sugang_helper.campus.schedule.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import bhoon.sugang_helper.campus.schedule.dto.request.ScheduleRequest;
import bhoon.sugang_helper.campus.schedule.dto.response.ScheduleResponse;
import bhoon.sugang_helper.campus.schedule.entity.Schedule;
import bhoon.sugang_helper.campus.schedule.repository.ScheduleRepository;
import bhoon.sugang_helper.common.error.CustomException;
import bhoon.sugang_helper.common.error.ErrorCode;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class ScheduleServiceTest {

    private static final String ERROR_CODE_FIELD = "errorCode";

    private static final LocalDate TODAY = LocalDate.of(2026, 10, 26);
    private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");
    private static final String TITLE = "수강신청";
    private static final String ONGOING = "진행 중";
    private static final String D_DAY = "D-Day";
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-10-26T01:00:00Z"), ZoneOffset.UTC);

    @Mock
    private ScheduleRepository scheduleRepository;
    private ScheduleService service;

    @BeforeEach
    void setUp() {
        service = new ScheduleService(scheduleRepository, CLOCK);
    }

    @Test
    void generalListAndDashboardKeepUnselectedSchedulesAndDateBasedLabels() {
        Schedule ongoing = schedule(1, TODAY.minusDays(1), TODAY, null, LocalTime.of(9, 0), false);
        Schedule future = schedule(2, TODAY.plusDays(3), TODAY.plusDays(4), null, null, false);
        when(scheduleRepository.findUpcomingSchedules(TODAY)).thenReturn(List.of(ongoing, future));

        assertThat(service.getUpcomingSchedules()).extracting(ScheduleResponse::getDDay)
                .containsExactly(D_DAY, "D-3");
        assertThat(service.findUpcomingSchedules()).extracting(summary -> summary.id())
                .containsExactly(1L, 2L);
    }

    @Test
    void selectedListSortsOngoingByEndThenFutureByStartAndId() {
        Schedule first = schedule(2, TODAY.minusDays(1), TODAY, null, LocalTime.of(11, 0), true);
        Schedule second = schedule(1, TODAY.minusDays(2), TODAY, null, LocalTime.of(12, 0), true);
        Schedule tied = schedule(4, TODAY, TODAY, LocalTime.of(13, 0), LocalTime.of(14, 0), true);
        Schedule earlierId = schedule(3, TODAY, TODAY, LocalTime.of(13, 0), LocalTime.of(14, 0), true);
        Schedule future = schedule(5, TODAY.plusDays(2), TODAY.plusDays(3), null, null, true);
        Schedule ended = schedule(6, TODAY, TODAY, null, LocalTime.of(10, 0), true);
        selected(List.of(tied, future, second, ended, earlierId, first));

        List<ScheduleResponse> response = service.getDdaySchedules();

        assertThat(response).extracting(ScheduleResponse::getId).containsExactly(2L, 1L, 3L, 4L, 5L);
        assertThat(response).extracting(ScheduleResponse::getDDay)
                .containsExactly(ONGOING, ONGOING, D_DAY, D_DAY, "D-2");
    }

    @ParameterizedTest
    @CsvSource({
            "2026-10-26T08:59:59, true, D-Day",
            "2026-10-26T09:00:00, true, 진행 중",
            "2026-10-26T10:59:59, true, 진행 중",
            "2026-10-26T11:00:00, false, unused"
    })
    void explicitTimeUsesInclusiveStartAndExclusiveEnd(String localNow, boolean visible, String expectedLabel) {
        Schedule schedule = schedule(1, TODAY, TODAY, LocalTime.of(9, 0), LocalTime.of(11, 0), true);
        selected(List.of(schedule));
        service = at(LocalDateTime.parse(localNow));

        List<ScheduleResponse> response = service.getDdaySchedules();

        assertThat(response).hasSize(visible ? 1 : 0);
        if (visible) {
            assertThat(response.getFirst().getDDay()).isEqualTo(expectedLabel);
        }
    }

    @ParameterizedTest
    @CsvSource({
            "2026-10-25T23:59:59, D-1",
            "2026-10-26T00:00:00, 진행 중",
            "2026-10-26T23:59:59, 진행 중"
    })
    void allDayPeriodIncludesWholeEndDate(String localNow, String expectedLabel) {
        Schedule schedule = schedule(1, TODAY, TODAY, null, null, true);
        LocalDateTime now = LocalDateTime.parse(localNow);
        when(scheduleRepository.findByDdayEnabledTrueAndDeletedAtIsNullAndEndDateGreaterThanEqual(now.toLocalDate()))
                .thenReturn(List.of(schedule));
        service = at(now);

        assertThat(service.getDdaySchedules()).singleElement()
                .extracting(ScheduleResponse::getDDay).isEqualTo(expectedLabel);
    }

    @Test
    void allDayAndZeroLengthPeriodsAreExcludedAtTheirEnd() {
        Schedule allDay = schedule(1, TODAY.minusDays(1), TODAY.minusDays(1), null, null, true);
        Schedule point = schedule(2, TODAY, TODAY, LocalTime.of(10, 0), LocalTime.of(10, 0), true);
        selected(List.of(allDay, point));

        assertThat(new ScheduleRequest(TITLE, TODAY, TODAY, LocalTime.of(10, 0), LocalTime.of(10, 0), true)
                .isValidPeriod()).isTrue();
        assertThat(allDay.endsAt()).isEqualTo(TODAY.atStartOfDay());
        assertThat(service.getDdaySchedules()).isEmpty();
    }

    @Test
    void seoulDateIsUsedEvenWhenClockAndServerAreOnUtcPreviousDay() {
        service = new ScheduleService(scheduleRepository,
                Clock.fixed(Instant.parse("2026-10-25T15:00:00Z"), ZoneOffset.UTC));
        Schedule schedule = schedule(1, TODAY, TODAY, null, null, true);
        selected(List.of(schedule));
        when(scheduleRepository.findUpcomingSchedules(TODAY)).thenReturn(List.of(schedule));

        assertThat(service.getDdaySchedules()).singleElement()
                .extracting(ScheduleResponse::getDDay).isEqualTo(ONGOING);
        assertThat(service.getUpcomingSchedules()).singleElement()
                .extracting(ScheduleResponse::getDDay).isEqualTo(D_DAY);
    }

    @Test
    void createAndEditKeepSelectionAndUpdatedTimesInResponses() {
        when(scheduleRepository.save(any(Schedule.class))).thenAnswer(invocation -> invocation.getArgument(0));
        ScheduleRequest request = new ScheduleRequest(TITLE, TODAY, TODAY.plusDays(1), null, null, true);

        assertThat(service.createSchedule(request).isDdayEnabled()).isTrue();
        assertThat(service.createSchedule(new ScheduleRequest(TITLE, TODAY, TODAY, null, null)).isDdayEnabled())
                .isFalse();
        Schedule existing = schedule(1, TODAY, TODAY.plusDays(1), null, null, true);
        when(scheduleRepository.findById(1L)).thenReturn(Optional.of(existing));
        ScheduleRequest omittedSelection = new ScheduleRequest(TITLE, TODAY, TODAY.plusDays(1), null, null);
        assertThat(service.updateSchedule(1L, omittedSelection).isDdayEnabled()).isTrue();
        ScheduleRequest edit = new ScheduleRequest("변경된 일정", TODAY.plusDays(2), TODAY.plusDays(3),
                LocalTime.of(12, 0), LocalTime.of(18, 0), false);
        ScheduleResponse updated = service.updateSchedule(1L, edit);

        assertThat(updated.getScheduleType()).isEqualTo("변경된 일정");
        assertThat(updated.getStartTime()).isEqualTo(LocalTime.of(12, 0));
        assertThat(updated.getDDay()).isEqualTo("D-2");
        assertThat(updated.isDdayEnabled()).isFalse();
    }

    @Test
    void deletedScheduleCannotBeEditedOrDeletedAgainAndRestoreKeepsSelection() {
        Schedule schedule = schedule(1, TODAY, TODAY, null, null, true);
        when(scheduleRepository.findById(1L)).thenReturn(Optional.of(schedule));
        service.deleteSchedule(1L);

        assertThat(schedule.getDeletedAt()).isEqualTo(LocalDateTime.of(TODAY, LocalTime.of(10, 0)));
        assertThatThrownBy(() -> service.updateSchedule(1L,
                new ScheduleRequest(TITLE, TODAY, TODAY, null, null, false)))
                .isInstanceOf(CustomException.class).hasFieldOrPropertyWithValue(ERROR_CODE_FIELD, ErrorCode.NOT_FOUND);
        assertThatThrownBy(() -> service.deleteSchedule(1L))
                .isInstanceOf(CustomException.class).hasFieldOrPropertyWithValue(ERROR_CODE_FIELD, ErrorCode.NOT_FOUND);
        ScheduleResponse restored = service.restoreSchedule(1L);
        assertThat(restored.getDeletedAt()).isNull();
        assertThat(restored.isDdayEnabled()).isTrue();
    }

    @Test
    void adminListRetainsPastAndDeletedSchedulesForRestoration() {
        Schedule schedule = schedule(1, TODAY.minusDays(4), TODAY.minusDays(2), null, null, true);
        schedule.delete(LocalDateTime.now(CLOCK));
        when(scheduleRepository.findAllByOrderByStartDateDescStartTimeDesc()).thenReturn(List.of(schedule));

        assertThat(service.getAllSchedules()).singleElement().satisfies(response -> {
            assertThat(response.getDDay()).isEqualTo("D+2");
            assertThat(response.getDeletedAt()).isNotNull();
        });
    }

    @Test
    void missingScheduleAndInvertedPeriodAreRejected() {
        when(scheduleRepository.findById(1L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.restoreSchedule(1L)).isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue(ERROR_CODE_FIELD, ErrorCode.NOT_FOUND);
        assertThatThrownBy(() -> service.createSchedule(new ScheduleRequest(TITLE, TODAY.plusDays(1), TODAY,
                null, null, true))).isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue(ERROR_CODE_FIELD, ErrorCode.INVALID_INPUT);
        assertThatThrownBy(() -> service.createSchedule(new ScheduleRequest(TITLE, TODAY, TODAY,
                LocalTime.NOON, LocalTime.of(11, 0), true))).isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue(ERROR_CODE_FIELD, ErrorCode.INVALID_INPUT);
    }

    private ScheduleService at(LocalDateTime localNow) {
        return new ScheduleService(scheduleRepository,
                Clock.fixed(localNow.atZone(SEOUL).toInstant(), ZoneOffset.UTC));
    }

    private void selected(List<Schedule> schedules) {
        when(scheduleRepository.findByDdayEnabledTrueAndDeletedAtIsNullAndEndDateGreaterThanEqual(TODAY))
                .thenReturn(schedules);
    }

    private Schedule schedule(long id, LocalDate start, LocalDate end, LocalTime startTime, LocalTime endTime,
                              boolean ddayEnabled) {
        Schedule schedule = Schedule.builder().scheduleType(TITLE).startDate(start).endDate(end)
                .startTime(startTime).endTime(endTime).ddayEnabled(ddayEnabled).build();
        ReflectionTestUtils.setField(schedule, "id", id);
        return schedule;
    }
}
