package bhoon.sugang_helper.planning.timetable.dto.response;

import bhoon.sugang_helper.planning.timetable.dto.result.TimetableListItem;
import bhoon.sugang_helper.planning.timetable.entity.Timetable;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class TimetableResponse {
    private final Long id;
    private final String name;
    private final boolean isPrimary;
    private final long courseCount;

    public static TimetableResponse from(TimetableListItem timetable) {
        return new TimetableResponse(timetable.id(), timetable.name(), timetable.primary(), timetable.courseCount());
    }

    public static TimetableResponse from(Timetable timetable) {
        return new TimetableResponse(
                timetable.getId(),
                timetable.getName(),
                timetable.isPrimary(),
                timetable.getEntries().size());
    }
}
