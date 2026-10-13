package bhoon.sugang_helper.planning.timetable.repository;

import bhoon.sugang_helper.planning.timetable.entity.Timetable;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TimetableRepository extends JpaRepository<Timetable, Long> {
    List<Timetable> findByUserId(Long userId);

    List<Timetable> findByUserIdAndIsPrimaryTrue(Long userId);

    long countByUserId(Long userId);
}
