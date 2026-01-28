package bhoon.sugang_helper.domain.timetable.repository;

import bhoon.sugang_helper.domain.timetable.entity.TimetableEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface TimetableEntryRepository extends JpaRepository<TimetableEntry, Long> {
    Optional<TimetableEntry> findByTimetableIdAndCourseKey(Long timetableId, String courseKey);

    long countByTimetableId(Long timetableId);
}
