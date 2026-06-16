package bhoon.sugang_helper.timetable.infra;

import bhoon.sugang_helper.timetable.domain.TimetableEntry;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TimetableEntryRepository extends JpaRepository<TimetableEntry, Long> {
    Optional<TimetableEntry> findByTimetableIdAndCourseKey(Long timetableId, String courseKey);
}
