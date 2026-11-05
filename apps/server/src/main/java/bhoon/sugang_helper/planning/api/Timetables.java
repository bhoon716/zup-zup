package bhoon.sugang_helper.planning.api;

import java.util.Optional;

public interface Timetables {

    Optional<PrimaryTimetableSnapshot> findPrimaryForCurrentUser();
}
