package bhoon.sugang_helper.campus.schedule.repository;

import bhoon.sugang_helper.campus.schedule.entity.Schedule;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ScheduleRepository extends JpaRepository<Schedule, Long> {

    @Query("SELECT s FROM Schedule s WHERE s.deletedAt IS NULL AND s.endDate >= :today "
            + "ORDER BY s.startDate ASC, s.startTime ASC")
    List<Schedule> findUpcomingSchedules(@Param("today") LocalDate today);

    List<Schedule> findAllByOrderByStartDateDescStartTimeDesc();

    List<Schedule> findByDdayEnabledTrueAndDeletedAtIsNullAndEndDateGreaterThanEqual(LocalDate today);
}
