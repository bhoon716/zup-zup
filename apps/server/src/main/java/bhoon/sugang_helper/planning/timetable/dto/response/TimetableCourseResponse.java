package bhoon.sugang_helper.planning.timetable.dto.response;

import bhoon.sugang_helper.academic.course.dto.CourseSummary;
import java.util.List;
import java.util.stream.Collectors;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class TimetableCourseResponse {
    private String courseKey;
    private String name;
    private String professor;
    private String classTime;
    private String credits;
    private String classification;
    private String classroom;
    private List<TimetableCourseScheduleResponse> schedules;

    public static TimetableCourseResponse of(CourseSummary course) {
        return TimetableCourseResponse.builder()
                .courseKey(course.courseKey())
                .name(course.name())
                .professor(course.professor())
                .classTime(course.classTime())
                .credits(course.credits())
                .classification(course.classification())
                .classroom(course.classroom())
                .schedules(course.schedules().stream()
                        .map(TimetableCourseScheduleResponse::of)
                        .collect(Collectors.toList()))
                .build();
    }
}
