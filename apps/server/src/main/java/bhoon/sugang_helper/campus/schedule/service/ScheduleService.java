package bhoon.sugang_helper.campus.schedule.service;

import bhoon.sugang_helper.campus.schedule.dto.response.ScheduleSummary;
import bhoon.sugang_helper.campus.schedule.dto.request.ScheduleRequest;
import bhoon.sugang_helper.campus.schedule.dto.response.ScheduleResponse;
import bhoon.sugang_helper.campus.schedule.entity.Schedule;
import bhoon.sugang_helper.campus.schedule.repository.ScheduleRepository;
import bhoon.sugang_helper.common.error.CustomException;
import bhoon.sugang_helper.common.error.ErrorCode;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ScheduleService {

    private final ScheduleRepository scheduleRepository;
    private final Clock clock;

    @Autowired
    public ScheduleService(ScheduleRepository scheduleRepository) {
        this(scheduleRepository, Clock.system(ZoneId.of("Asia/Seoul")));
    }

    ScheduleService(ScheduleRepository scheduleRepository, Clock clock) {
        this.scheduleRepository = scheduleRepository;
        this.clock = clock.withZone(ZoneId.of("Asia/Seoul"));
    }

    public List<ScheduleResponse> getUpcomingSchedules() {
        LocalDateTime now = LocalDateTime.now(clock);
        return scheduleRepository.findUpcomingSchedules(now.toLocalDate())
                .stream()
                .map(schedule -> ScheduleResponse.from(schedule, now.toLocalDate()))
                .toList();
    }

    public List<ScheduleResponse> getDdaySchedules() {
        LocalDateTime now = LocalDateTime.now(clock);
        return scheduleRepository.findByDdayEnabledTrueAndDeletedAtIsNullAndEndDateGreaterThanEqual(now.toLocalDate())
                .stream()
                .filter(schedule -> now.isBefore(schedule.endsAt()))
                .sorted(Comparator.comparing((Schedule schedule) -> !schedule.isOngoingAt(now))
                        .thenComparing(schedule -> schedule.isOngoingAt(now) ? schedule.endsAt() : schedule.startsAt())
                        .thenComparing(Schedule::getId))
                .map(schedule -> ScheduleResponse.fromDday(schedule, now))
                .toList();
    }

    public List<ScheduleSummary> findUpcomingSchedules() {
        return getUpcomingSchedules().stream()
                .map(schedule -> new ScheduleSummary(
                        schedule.getId(),
                        schedule.getScheduleType(),
                        schedule.getStartDate(),
                        schedule.getEndDate(),
                        schedule.getStartTime(),
                        schedule.getEndTime(),
                        schedule.getDDay()))
                .toList();
    }

    public List<ScheduleResponse> getAllSchedules() {
        LocalDateTime now = LocalDateTime.now(clock);
        return scheduleRepository.findAllByOrderByStartDateDescStartTimeDesc()
                .stream()
                .map(schedule -> ScheduleResponse.from(schedule, now.toLocalDate()))
                .toList();
    }

    @Transactional
    public ScheduleResponse createSchedule(ScheduleRequest request) {
        validatePeriod(request);
        Schedule schedule = Schedule.builder()
                .scheduleType(request.getScheduleType())
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .ddayEnabled(Boolean.TRUE.equals(request.getDdayEnabled()))
                .build();
        return ScheduleResponse.from(scheduleRepository.save(schedule), LocalDateTime.now(clock).toLocalDate());
    }

    @Transactional
    public ScheduleResponse updateSchedule(Long id, ScheduleRequest request) {
        Schedule schedule = getActiveSchedule(id);
        validatePeriod(request);
        schedule.update(request.getScheduleType(), request.getStartDate(),
                request.getEndDate(), request.getStartTime(), request.getEndTime(),
                request.getDdayEnabled() == null ? schedule.isDdayEnabled() : request.getDdayEnabled());
        return ScheduleResponse.from(schedule, LocalDateTime.now(clock).toLocalDate());
    }

    @Transactional
    public void deleteSchedule(Long id) {
        getActiveSchedule(id).delete(LocalDateTime.now(clock));
    }

    @Transactional
    public ScheduleResponse restoreSchedule(Long id) {
        Schedule schedule = getScheduleEntity(id);
        schedule.restore();
        return ScheduleResponse.from(schedule, LocalDateTime.now(clock).toLocalDate());
    }

    private Schedule getActiveSchedule(Long id) {
        Schedule schedule = getScheduleEntity(id);
        if (schedule.getDeletedAt() != null) {
            throw new CustomException(ErrorCode.NOT_FOUND, "해당 일정을 찾을 수 없습니다.");
        }
        return schedule;
    }

    private void validatePeriod(ScheduleRequest request) {
        if (!request.isValidPeriod()) {
            throw new CustomException(ErrorCode.INVALID_INPUT, "종료 일시는 시작 일시보다 빠를 수 없습니다.");
        }
    }

    private Schedule getScheduleEntity(Long id) {
        return scheduleRepository.findById(id)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND, "해당 일정을 찾을 수 없습니다."));
    }
}
