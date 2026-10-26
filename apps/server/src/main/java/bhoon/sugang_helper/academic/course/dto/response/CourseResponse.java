package bhoon.sugang_helper.academic.course.dto.response;

import bhoon.sugang_helper.academic.course.entity.Course;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class CourseResponse {

    private final String courseKey;

    private final String subjectCode;

    private final String stdtrNo;

    private final String name;

    private final String classNumber;

    private final String professor;

    private final Integer capacity;

    private final Integer current;

    private final Integer available;

    private final String classification;

    private final String classTime;

    private final String credits;

    private final String classroom;

    private final String status;

    private final Boolean isSubscribable;

    private final Float averageRating;

    private final Integer reviewCount;

    private final LocalDateTime lastCrawledAt;




    public static CourseResponse from(Course course, String currentYear, String currentSemester,
                                      LocalDateTime lastCrawledAt) {

        String status = course.getAvailable() > 0 ? "AVAILABLE" : "FULL";

        boolean isSubscribable = course.isMatchingTarget(currentYear, currentSemester);

        return CourseResponse.builder()
                .courseKey(course.getCourseKey())
                .subjectCode(course.getSubjectCode())
                .stdtrNo(course.getStdtrNo())
                .name(course.getName())
                .classNumber(course.getClassNumber())
                .professor(course.getProfessor())
                .capacity(course.getCapacity())
                .current(course.getCurrent())
                .available(course.getAvailable())
                .classification(course.getClassification() != null ? course.getClassification().getDescription() : null)
                .classTime(course.getClassTime())
                .credits(course.getCredits())
                .classroom(course.getClassroom())
                .status(status)
                .averageRating(course.getAverageRating())
                .reviewCount(course.getReviewCount())
                .lastCrawledAt(lastCrawledAt)
                .isSubscribable(isSubscribable)
                .build();
    }
}
