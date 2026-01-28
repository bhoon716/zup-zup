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

    public static TimetableCourseResponse of(Course course) {
        return TimetableCourseResponse.builder()
                .courseKey(course.getCourseKey())
                .name(course.getName())
                .professor(course.getProfessor())
                .classTime(course.getClassTime())
                .credits(course.getCredits())
                .classification(course.getClassification() != null ? course.getClassification().name() : null)
                .classroom(course.getClassroom())
                .build();
    }
}
