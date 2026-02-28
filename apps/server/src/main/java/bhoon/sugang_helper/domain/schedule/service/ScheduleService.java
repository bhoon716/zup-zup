package bhoon.sugang_helper.domain.schedule.service;

import bhoon.sugang_helper.common.error.CustomException;
import bhoon.sugang_helper.common.error.ErrorCode;
import bhoon.sugang_helper.domain.schedule.entity.Schedule;
import bhoon.sugang_helper.domain.schedule.repository.ScheduleRepository;
import bhoon.sugang_helper.domain.schedule.request.ScheduleRequest;
import bhoon.sugang_helper.domain.schedule.response.ScheduleResponse;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ScheduleService {

    private final ScheduleRepository scheduleRepository;

    /**
     * 유저용: 날짜가 지나지 않은(오늘 포함) 진행 중 또는 예정된 일정 목록을 조회
     */
    public List<ScheduleResponse> getUpcomingSchedules() {
        return scheduleRepository.findUpcomingSchedules(LocalDate.now())
                .stream()
                .map(ScheduleResponse::from)
                .toList();
    }

    /**
     * 어드민용: 만료된 일정을 포함한 모든 일정을 최신순으로 조회
     */
    public List<ScheduleResponse> getAllSchedules() {
        return scheduleRepository.findAllByOrderByScheduleDateDescScheduleTimeDesc()
                .stream()
                .map(ScheduleResponse::from)
                .toList();
    }

    /**
     * 새로운 일정을 생성
     */
    @Transactional
    public ScheduleResponse createSchedule(ScheduleRequest request) {
        Schedule schedule = Schedule.builder()
                .title(request.getTitle())
                .scheduleDate(request.getScheduleDate())
                .scheduleTime(request.getScheduleTime())
                .build();
        return ScheduleResponse.from(scheduleRepository.save(schedule));
    }

    /**
     * 특정 일정을 수정
     */
    @Transactional
    public ScheduleResponse updateSchedule(Long id, ScheduleRequest request) {
        Schedule schedule = getScheduleEntity(id);
        schedule.update(request.getTitle(), request.getScheduleDate(), request.getScheduleTime());
        return ScheduleResponse.from(schedule);
    }

    /**
     * 특정 일정을 삭제
     */
    @Transactional
    public void deleteSchedule(Long id) {
        Schedule schedule = getScheduleEntity(id);
        scheduleRepository.delete(schedule);
    }

    private Schedule getScheduleEntity(Long id) {
        return scheduleRepository.findById(id)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND, "해당 일정을 찾을 수 없습니다."));
    }
}
