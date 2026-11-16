package bhoon.sugang_helper.academic.course.dto.response;

import bhoon.sugang_helper.academic.course.entity.Course;
import bhoon.sugang_helper.academic.course.enums.CourseAccreditation;
import bhoon.sugang_helper.academic.course.enums.CourseClassification;
import bhoon.sugang_helper.academic.course.enums.CourseStatus;
import bhoon.sugang_helper.academic.course.enums.DisclosureStatus;
import bhoon.sugang_helper.academic.course.enums.GradingMethod;
import bhoon.sugang_helper.academic.course.enums.LectureLanguage;
import bhoon.sugang_helper.academic.course.enums.TargetGrade;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import lombok.Getter;

@Getter
public class CourseDetailResponse {

    private final String courseKey;

    private final String subjectCode;

    private final String stdtrNo;

    private final String name;

    private final String classNumber;

    private final String professor;

    private final TargetGrade targetGrade;

    private final Integer capacity;

    private final Integer current;

    private final Integer available;

    private final String classification;

    private final String department;

    private final String gradingMethod;

    private final String lectureLanguage;

    private final String classTime;

    private final String credits;

    private final Integer lectureHours;

    private final String disclosure;

    private final String disclosureReason;

    private final String generalCategory;

    private final String generalDetail;

    private final String accreditation;

    private final String courseStatus;

    private final String classroom;

    private final Boolean hasSyllabus;

    private final String generalCategoryByYear;

    private final String courseDirection;

    private final String classDuration;

    private final String status;

    private final Boolean isSubscribable;

    private final Float averageRating;

    private final Integer reviewCount;

    private final Boolean isReviewed;

    private final List<ScheduleResponse> schedules;

    private final LocalDateTime lastCrawledAt;

    private CourseDetailResponse(Course course, String currentYear, String currentSemester,
                                 boolean isReviewed, LocalDateTime lastCrawledAt) {
        String status = resolveSeatStatus(course.getAvailable());
        boolean isSubscribable = course.isMatchingTarget(currentYear, currentSemester);

        this.courseKey = course.getCourseKey();
        this.subjectCode = course.getSubjectCode();
        this.stdtrNo = course.getStdtrNo();
        this.name = course.getName();
        this.classNumber = course.getClassNumber();
        this.professor = course.getProfessor();
        this.targetGrade = course.getTargetGrade();
        this.capacity = course.getCapacity();
        this.current = course.getCurrent();
        this.available = course.getAvailable();
        this.classification = Optional.ofNullable(course.getClassification()).map(CourseClassification::getDescription).orElse(null);
        this.department = course.getDepartment();
        this.gradingMethod = Optional.ofNullable(course.getGradingMethod()).map(GradingMethod::getDescription).orElse(null);
        this.lectureLanguage = Optional.ofNullable(course.getLectureLanguage()).map(LectureLanguage::getValue).orElse(null);
        this.classTime = course.getClassTime();
        this.credits = course.getCredits();
        this.lectureHours = course.getLectureHours();
        this.disclosure = Optional.ofNullable(course.getDisclosure()).map(DisclosureStatus::getDescription).orElse(null);
        this.disclosureReason = course.getDisclosureReason();
        this.generalCategory = course.getGeneralCategory();
        this.generalDetail = course.getGeneralDetail();
        this.accreditation = Optional.ofNullable(course.getAccreditation()).map(CourseAccreditation::getDescription).orElse(null);
        this.courseStatus = Optional.ofNullable(course.getStatus()).map(CourseStatus::getDescription).orElse(null);
        this.classroom = course.getClassroom();
        this.hasSyllabus = course.getHasSyllabus();
        this.generalCategoryByYear = course.getGeneralCategoryByYear();
        this.courseDirection = course.getCourseDirection();
        this.classDuration = course.getClassDuration();
        this.status = status;
        this.isSubscribable = isSubscribable;
        this.averageRating = course.getAverageRating();
        this.reviewCount = course.getReviewCount();
        this.isReviewed = isReviewed;
        this.lastCrawledAt = lastCrawledAt;
        this.schedules = course.getSchedules().stream()
                .map(ScheduleResponse::from)
                .toList();
    }

    public static CourseDetailResponse from(Course course, String currentYear, String currentSemester,
                                            boolean isReviewed, LocalDateTime lastCrawledAt) {
        return new CourseDetailResponse(course, currentYear, currentSemester, isReviewed, lastCrawledAt);
    }

    private static String resolveSeatStatus(Integer available) {
        if (available > 0) {
            return "AVAILABLE";
        }
        return "FULL";
    }
}
