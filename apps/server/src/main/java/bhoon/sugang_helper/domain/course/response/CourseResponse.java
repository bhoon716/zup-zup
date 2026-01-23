package bhoon.sugang_helper.domain.course.response;

import bhoon.sugang_helper.domain.course.entity.Course;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class CourseResponse {

    private final String courseKey;
    private final String subjectCode;
    private final String name;
    private final String classNumber;
    private final String professor;
    private final Integer capacity;
    private final Integer current;
    private final Integer available;

    public static CourseResponse from(Course course) {
        return new CourseResponse(
                course.getCourseKey(),
                course.getSubjectCode(),
                course.getName(),
                course.getClassNumber(),
                course.getProfessor(),
                course.getCapacity(),
                course.getCurrent(),
                course.getAvailable());
    }
}
