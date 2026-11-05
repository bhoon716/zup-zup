package bhoon.sugang_helper.planning.api;

import java.util.Optional;

/** 다른 모듈이 사용하는 Timetables 계약. */
public interface Timetables {

    Optional<PrimaryTimetableSnapshot> findPrimaryForCurrentUser();
}
