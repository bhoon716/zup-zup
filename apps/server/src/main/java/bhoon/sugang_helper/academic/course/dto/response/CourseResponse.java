package bhoon.sugang_helper.academic.course.dto.response;

import bhoon.sugang_helper.academic.course.entity.Course;
import bhoon.sugang_helper.academic.course.enums.CourseClassification;
import bhoon.sugang_helper.academic.course.enums.DisclosureStatus;
import java.time.LocalDateTime;
import java.util.Optional;
import lombok.Getter;

@Getter
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

    private final String disclosure;

    private final String classroom;

    private final String status;

    private final Boolean isSubscribable;

    private final Float averageRating;

    private final Integer reviewCount;

    private final LocalDateTime lastCrawledAt;

    private CourseResponse(Course course, String currentYear, String currentSemester,
                           LocalDateTime lastCrawledAt) {
        String status = resolveSeatStatus(course.getAvailable());
        boolean isSubscribable = course.isMatchingTarget(currentYear, currentSemester);

        this.courseKey = course.getCourseKey();
        this.subjectCode = course.getSubjectCode();
        this.stdtrNo = course.getStdtrNo();
        this.name = course.getName();
        this.classNumber = course.getClassNumber();
        this.professor = course.getProfessor();
        this.capacity = course.getCapacity();
        this.current = course.getCurrent();
        this.available = course.getAvailable();
        this.classification = Optional.ofNullable(course.getClassification()).map(CourseClassification::getDescription).orElse(null);
        this.classTime = course.getClassTime();
        this.credits = course.getCredits();
        this.disclosure = Optional.ofNullable(course.getDisclosure()).map(DisclosureStatus::getDescription).orElse(null);
        this.classroom = course.getClassroom();
        this.status = status;
        this.averageRating = course.getAverageRating();
        this.reviewCount = course.getReviewCount();
        this.lastCrawledAt = lastCrawledAt;
        this.isSubscribable = isSubscribable;
    }

    public static CourseResponse from(Course course, String currentYear, String currentSemester,
                                      LocalDateTime lastCrawledAt) {
        return new CourseResponse(course, currentYear, currentSemester, lastCrawledAt);
    }

    private static String resolveSeatStatus(Integer available) {
        if (available > 0) {
            return "AVAILABLE";
        }
        return "FULL";
    }
}
