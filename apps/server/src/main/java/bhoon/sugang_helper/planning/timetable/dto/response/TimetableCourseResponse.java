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
    private List<ScheduleResponse> schedules;

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
                        .map(ScheduleResponse::of)
                        .collect(Collectors.toList()))
                .build();
    }

    @Getter
    @Builder
    public static class ScheduleResponse {
        private String dayOfWeek;
        private String startTime;
        private String endTime;

        public static ScheduleResponse of(CourseSummary.CourseScheduleSummary schedule) {
            return ScheduleResponse.builder()
                    .dayOfWeek(schedule.dayOfWeek())
                    .startTime(schedule.startTime())
                    .endTime(schedule.endTime())
                    .build();
        }
    }
}
