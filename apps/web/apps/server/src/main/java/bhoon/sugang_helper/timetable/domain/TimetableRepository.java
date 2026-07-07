package bhoon.sugang_helper.timetable.domain;

import bhoon.sugang_helper.common.domain.RepositoryContract;
import java.util.List;

public interface TimetableRepository extends RepositoryContract<Timetable, Long> {
    List<Timetable> findByUserId(Long userId);

    List<Timetable> findByUserIdAndIsPrimaryTrue(Long userId);

    long countByUserId(Long userId);
}
