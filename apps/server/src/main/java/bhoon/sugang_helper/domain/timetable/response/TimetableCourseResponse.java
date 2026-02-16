package bhoon.sugang_helper.domain.timetable.response;

import bhoon.sugang_helper.domain.course.entity.Course;
import bhoon.sugang_helper.domain.course.entity.CourseSchedule;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.stream.Collectors;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(description = "시간표 강의 응답 DTO")
public class TimetableCourseResponse {
    @Schema(description = "강의 키", example = "2026:10:12345:01")
    private String courseKey;
    @Schema(description = "강의명", example = "자료구조")
    private String name;
    @Schema(description = "교수명", example = "홍길동")
    private String professor;
    @Schema(description = "원본 강의 시간 문자열", example = "월 1-A, 수 1-B")
    private String classTime;
    @Schema(description = "학점", example = "3")
    private String credits;
    @Schema(description = "이수 구분", example = "전공필수")
    private String classification;
    @Schema(description = "강의실", example = "공대 7호관 301")
    private String classroom;
    @Schema(description = "시간표 스케줄 목록")
    private List<ScheduleResponse> schedules;

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
                        .collect(Collectors.toList()))
                .build();
    }

    @Getter
    @Builder
    @Schema(description = "강의 시간 구간")
    public static class ScheduleResponse {
        @Schema(description = "요일", example = "MONDAY")
        private String dayOfWeek;
        @Schema(description = "시작 시간", example = "09:00")
        private String startTime;
        @Schema(description = "종료 시간", example = "10:00")
        private String endTime;

        public static ScheduleResponse of(CourseSchedule schedule) {
            return ScheduleResponse.builder()
                    .dayOfWeek(schedule.getDayOfWeek().name())
                    .startTime(schedule.getStartTime().toString())
                    .endTime(schedule.getEndTime().toString())
                    .build();
        }
    }
}
