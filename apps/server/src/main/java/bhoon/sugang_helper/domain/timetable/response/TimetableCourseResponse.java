package bhoon.sugang_helper.domain.timetable.response;

import bhoon.sugang_helper.domain.course.entity.Course;
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
    private java.util.List<ScheduleResponse> schedules;

    public static TimetableCourseResponse of(Course course) {
        return TimetableCourseResponse.builder()
                .courseKey(course.getCourseKey())
                .name(course.getName())
                .professor(course.getProfessor())
                .classTime(course.getClassTime())
                .credits(course.getCredits())
                .classification(course.getClassification() != null ? course.getClassification().name() : null)
                .classroom(course.getClassroom())
                .schedules(course.getSchedules().stream()
                        .map(ScheduleResponse::of)
                        .collect(java.util.stream.Collectors.toList()))
                .build();
    }

    @Getter
    @Builder
    public static class ScheduleResponse {
        private String dayOfWeek;
        private String period;
        private String startTime;
        private String endTime;

        public static ScheduleResponse of(bhoon.sugang_helper.domain.course.entity.CourseSchedule schedule) {
            return ScheduleResponse.builder()
                    .dayOfWeek(schedule.getDayOfWeek().name())
                    .period(schedule.getPeriod().name())
                    .startTime(schedule.getStartTime().toString())
                    .endTime(schedule.getEndTime().toString())
                    .build();
        }
    }
}
