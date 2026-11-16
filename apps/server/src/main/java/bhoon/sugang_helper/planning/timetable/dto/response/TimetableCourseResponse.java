package bhoon.sugang_helper.planning.timetable.dto.response;

import bhoon.sugang_helper.academic.api.catalog.CourseSummary;
import java.util.List;
import java.util.stream.Collectors;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class TimetableCourseResponse {
    private final String courseKey;
    private final String name;
    private final String professor;
    private final String classTime;
    private final String credits;
    private final String classification;
    private final String classroom;
    private final List<TimetableCourseScheduleResponse> schedules;

    public static TimetableCourseResponse from(CourseSummary course) {
        return new TimetableCourseResponse(
                course.courseKey(),
                course.name(),
                course.professor(),
                course.classTime(),
                course.credits(),
                course.classification(),
                course.classroom(),
                course.schedules().stream()
                        .map(TimetableCourseScheduleResponse::from)
                        .collect(Collectors.toList()));
    }
}
