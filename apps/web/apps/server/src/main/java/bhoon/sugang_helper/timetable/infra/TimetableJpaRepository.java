package bhoon.sugang_helper.timetable.infra;

import bhoon.sugang_helper.timetable.domain.Timetable;
import bhoon.sugang_helper.timetable.domain.TimetableRepository;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TimetableJpaRepository extends JpaRepository<Timetable, Long>, TimetableRepository {
    List<Timetable> findByUserId(Long userId);

    List<Timetable> findByUserIdAndIsPrimaryTrue(Long userId);

    long countByUserId(Long userId);
}
