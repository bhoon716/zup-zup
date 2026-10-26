package bhoon.sugang_helper.planning.timetable.service;

import bhoon.sugang_helper.planning.timetable.dto.PrimaryTimetableSnapshot;
import java.util.Optional;

public interface PrimaryTimetableQuery {

    Optional<PrimaryTimetableSnapshot> findPrimaryForCurrentUser();
}
