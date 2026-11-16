package bhoon.sugang_helper.planning.timetable.dto.response;

import bhoon.sugang_helper.academic.api.catalog.CourseSummary;
import bhoon.sugang_helper.planning.timetable.entity.CustomSchedule;
import bhoon.sugang_helper.planning.timetable.entity.Timetable;
import java.util.List;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class TimetableDetailResponse {
    private final Long id;
    private final String name;
    private final boolean isPrimary;
    private final List<TimetableCourseResponse> courses;
    private final List<CustomScheduleResponse> customSchedules;
    private final String totalCredits;

    public static TimetableDetailResponse from(Timetable timetable, List<CourseSummary> courses,
                                               List<CustomSchedule> customSchedules, String totalCredits) {
        return new TimetableDetailResponse(
                timetable.getId(),
                timetable.getName(),
                timetable.isPrimary(),
                courses.stream().map(TimetableCourseResponse::from).toList(),
                customSchedules.stream()
                        .map(CustomScheduleResponse::from)
                        .toList(),
                totalCredits);
    }
}
