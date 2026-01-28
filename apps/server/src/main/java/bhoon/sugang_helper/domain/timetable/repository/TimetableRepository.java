package bhoon.sugang_helper.domain.timetable.repository;

import bhoon.sugang_helper.domain.timetable.entity.Timetable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface TimetableRepository extends JpaRepository<Timetable, Long> {
    List<Timetable> findByUserId(Long userId);

    Optional<Timetable> findByUserIdAndIsPrimaryTrue(Long userId);

    long countByUserId(Long userId);
}
