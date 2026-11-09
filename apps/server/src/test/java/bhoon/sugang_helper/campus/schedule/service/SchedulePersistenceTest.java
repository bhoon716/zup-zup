package bhoon.sugang_helper.campus.schedule.service;

import static org.assertj.core.api.Assertions.assertThat;

import bhoon.sugang_helper.campus.schedule.dto.request.ScheduleRequest;
import bhoon.sugang_helper.campus.schedule.dto.response.ScheduleResponse;
import bhoon.sugang_helper.campus.schedule.entity.Schedule;
import bhoon.sugang_helper.campus.schedule.repository.ScheduleRepository;
import jakarta.persistence.EntityManager;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

@DataJpaTest
class SchedulePersistenceTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 10, 26);
    private static final String TITLE = "수강신청";

    @Autowired private ScheduleRepository schedules;

    @Autowired private EntityManager entityManager;

    private ScheduleService service;

    @BeforeEach
    void setUp() {
        service = new ScheduleService(schedules,
                Clock.fixed(Instant.parse("2026-10-26T01:00:00Z"), ZoneOffset.UTC));
    }

    @Test
    @DisplayName("공개 조회는 삭제된 일정을 제외하고 디데이 선택과 무관하게 일반 일정을 보여 준다")
    void publicQueriesExcludeDeletedRowsAndDdaySelectionDoesNotHideGeneralSchedules() {
        // given
        Long selected = save(TODAY, TODAY, null, true);
        Long unselected = save(TODAY.plusDays(1), TODAY.plusDays(2), null, false);
        Long endedToday = save(TODAY, TODAY, LocalTime.of(9, 0), true);
        Long past = save(TODAY.minusDays(2), TODAY.minusDays(1), null, true);
        Long deleted = save(TODAY, TODAY, null, true);
        service.deleteSchedule(deleted);
        flushAndClear();

        // when & then
        assertThat(service.getUpcomingSchedules()).extracting(ScheduleResponse::getId)
                .containsExactlyInAnyOrder(selected, endedToday, unselected);
        assertThat(service.getDdaySchedules()).extracting(ScheduleResponse::getId).containsExactly(selected);
        assertThat(service.getAllSchedules()).extracting(ScheduleResponse::getId)
                .containsExactlyInAnyOrder(selected, unselected, endedToday, past, deleted);

        service.restoreSchedule(deleted);
        flushAndClear();
        assertThat(service.getDdaySchedules()).extracting(ScheduleResponse::getId)
                .containsExactly(selected, deleted);
        assertThat(schedules.findById(deleted)).get().satisfies(restored -> {
            assertThat(restored.getDeletedAt()).isNull();
            assertThat(restored.isDdayEnabled()).isTrue();
        });
    }

    @Test
    @DisplayName("선택 여부와 기간 수정은 같은 저장된 일정에 반영한다")
    void editingSelectionAndPeriodUpdatesTheSamePersistedSchedule() {
        // given
        Long id = service.createSchedule(new ScheduleRequest(TITLE, TODAY, TODAY, null, null, true)).getId();
        flushAndClear();
        service.updateSchedule(id, new ScheduleRequest(TITLE, TODAY, TODAY, null, null, false));
        flushAndClear();

        // when & then
        assertThat(service.getDdaySchedules()).isEmpty();
        assertThat(service.getUpcomingSchedules()).hasSize(1);

        service.updateSchedule(id, new ScheduleRequest("변경 일정", TODAY.plusDays(3), TODAY.plusDays(4),
                LocalTime.of(13, 0), LocalTime.of(17, 0), true));
        flushAndClear();
        assertThat(service.getDdaySchedules()).singleElement().satisfies(response -> {
            assertThat(response.getId()).isEqualTo(id);
            assertThat(response.getScheduleType()).isEqualTo("변경 일정");
            assertThat(response.getDDay()).isEqualTo("D-3");
            assertThat(response.getStartTime()).isEqualTo(LocalTime.of(13, 0));
        });
    }

    private Long save(LocalDate start, LocalDate end, LocalTime endTime, boolean selected) {
        return schedules.save(Schedule.builder().scheduleType(TITLE).startDate(start).endDate(end)
                .endTime(endTime).ddayEnabled(selected).build()).getId();
    }

    private void flushAndClear() {
        entityManager.flush();
        entityManager.clear();
    }
}
