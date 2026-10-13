package bhoon.sugang_helper.planning.timetable.repository;

import bhoon.sugang_helper.planning.timetable.entity.TimetableEntry;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TimetableEntryRepository extends JpaRepository<TimetableEntry, Long> {
    Optional<TimetableEntry> findByTimetableIdAndCourseKey(Long timetableId, String courseKey);
}
