package bhoon.sugang_helper.academic.course.dto.response;

import bhoon.sugang_helper.academic.course.entity.Course;
import bhoon.sugang_helper.academic.course.enums.TargetGrade;
import java.time.LocalDateTime;
import java.util.List;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
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




    public static CourseDetailResponse from(Course course, String currentYear, String currentSemester,
                                            boolean isReviewed, LocalDateTime lastCrawledAt) {

        String status = course.getAvailable() > 0 ? "AVAILABLE" : "FULL";

        boolean isSubscribable = course.isMatchingTarget(currentYear, currentSemester);

        return CourseDetailResponse.builder()
                .courseKey(course.getCourseKey())
                .subjectCode(course.getSubjectCode())
                .stdtrNo(course.getStdtrNo())
                .name(course.getName())
                .classNumber(course.getClassNumber())
                .professor(course.getProfessor())
                .targetGrade(course.getTargetGrade())
                .capacity(course.getCapacity())
                .current(course.getCurrent())
                .available(course.getAvailable())
                .classification(course.getClassification() != null
                        ? course.getClassification().getDescription()
                        : null)
                .department(course.getDepartment())
                .gradingMethod(course.getGradingMethod() != null
                        ? course.getGradingMethod().getDescription()
                        : null)
                .lectureLanguage(
                        course.getLectureLanguage() != null
                                ? course.getLectureLanguage().getValue()
                                : null)
                .classTime(course.getClassTime())
                .credits(course.getCredits())
                .lectureHours(course.getLectureHours())
                .disclosure(course.getDisclosure() != null ? course.getDisclosure().getDescription()
                        : null)
                .disclosureReason(course.getDisclosureReason())
                .generalCategory(course.getGeneralCategory())
                .generalDetail(course.getGeneralDetail())
                .accreditation(course.getAccreditation() != null
                        ? course.getAccreditation().getDescription()
                        : null)
                .courseStatus(course.getStatus() != null ? course.getStatus().getDescription() : null)
                .classroom(course.getClassroom())
                .hasSyllabus(course.getHasSyllabus())
                .generalCategoryByYear(course.getGeneralCategoryByYear())
                .courseDirection(course.getCourseDirection())
                .classDuration(course.getClassDuration())
                .status(status)
                .isSubscribable(isSubscribable)
                .averageRating(course.getAverageRating())
                .reviewCount(course.getReviewCount())
                .isReviewed(isReviewed)
                .lastCrawledAt(lastCrawledAt)
                .schedules(course.getSchedules().stream()
                        .map(ScheduleResponse::from)
                        .toList())
                .build();
    }
}
