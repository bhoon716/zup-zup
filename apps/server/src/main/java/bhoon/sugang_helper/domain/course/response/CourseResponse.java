package bhoon.sugang_helper.domain.course.response;

import bhoon.sugang_helper.domain.course.entity.Course;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Schema(description = "과목 정보 응답 DTO")
public class CourseResponse {

    @Schema(description = "과목 키", example = "0000130844-1")
    private final String courseKey;

    @Schema(description = "과목 코드", example = "0000130844")
    private final String subjectCode;

    @Schema(description = "과목명", example = "(글로컬)우리생활과화학")
    private final String name;

    @Schema(description = "분반", example = "1")
    private final String classNumber;

    @Schema(description = "담당 교수", example = "김혜진")
    private final String professor;

    @Schema(description = "정원", example = "40")
    private final Integer capacity;

    @Schema(description = "현재 인원", example = "35")
    private final Integer current;

    @Schema(description = "잔여 공석", example = "5")
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
