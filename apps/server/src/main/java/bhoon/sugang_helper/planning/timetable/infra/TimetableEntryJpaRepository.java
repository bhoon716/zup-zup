package bhoon.sugang_helper.planning.timetable.infra;

import bhoon.sugang_helper.planning.timetable.domain.TimetableEntry;
import bhoon.sugang_helper.planning.timetable.domain.TimetableEntryRepository;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TimetableEntryJpaRepository extends JpaRepository<TimetableEntry, Long>, TimetableEntryRepository {
    Optional<TimetableEntry> findByTimetableIdAndCourseKey(Long timetableId, String courseKey);
}
