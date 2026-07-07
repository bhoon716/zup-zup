package bhoon.sugang_helper.timetable.domain;

import bhoon.sugang_helper.common.domain.RepositoryContract;
import java.util.Optional;

public interface TimetableEntryRepository extends RepositoryContract<TimetableEntry, Long> {
    Optional<TimetableEntry> findByTimetableIdAndCourseKey(Long timetableId, String courseKey);
}
