package bhoon.sugang_helper.domain.schedule.repository;

import bhoon.sugang_helper.domain.schedule.entity.Schedule;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ScheduleRepository extends JpaRepository<Schedule, Long> {

    /**
     * 유저에게 보여줄 때: 종료일이 오늘 날짜 이후(오늘 포함)인 일정만 최신순(시작일 기준)으로 조회
     */
    @Query("SELECT s FROM Schedule s WHERE s.endDate >= :today ORDER BY s.startDate ASC, s.startTime ASC")
    List<Schedule> findUpcomingSchedules(@Param("today") LocalDate today);

    /**
     * 어드민에게 보여줄 때: 모든 일정을 날짜순으로 조회
     */
    List<Schedule> findAllByOrderByStartDateDescStartTimeDesc();
}
