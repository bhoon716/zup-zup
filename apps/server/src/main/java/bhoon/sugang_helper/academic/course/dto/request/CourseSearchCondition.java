package bhoon.sugang_helper.academic.course.dto.request;

import bhoon.sugang_helper.academic.course.dto.criteria.CourseSearchCriteria;
import bhoon.sugang_helper.academic.course.dto.criteria.CourseSelectedSchedule;

import bhoon.sugang_helper.academic.course.enums.CourseClassification;
import bhoon.sugang_helper.academic.course.enums.CourseDayOfWeek;
import bhoon.sugang_helper.academic.course.enums.CourseStatus;
import bhoon.sugang_helper.academic.course.enums.DisclosureStatus;
import bhoon.sugang_helper.academic.course.enums.GradingMethod;
import bhoon.sugang_helper.academic.course.enums.LectureLanguage;
import bhoon.sugang_helper.academic.course.enums.TargetGrade;
import bhoon.sugang_helper.common.error.CustomException;
import bhoon.sugang_helper.common.error.ErrorCode;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalTime;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PUBLIC)
@ToString
public class CourseSearchCondition {

    private static final String DEFAULT_SORT_BY = "popular";
    private static final Set<String> WISHLIST_SORT_ALIASES = Set.of(
            "wishlist", "wishlistcount", "wishlist_count", "wished");

    @Size(max = 100)
    private String name;

    @Size(max = 100)
    private String keyword;

    @Size(max = 100)
    private String professor;

    @Size(max = 30)
    private String subjectCode;

    @Size(max = 4)
    private String academicYear;

    @Size(max = 30)
    private String semester;

    @Size(max = 20)
    private List<@Size(max = 50) String> classifications;

    @Size(max = 100)
    private String department;

    @Size(max = 20)
    private List<@Size(max = 50) String> gradingMethods;

    @Size(max = 20)
    private List<@Size(max = 50) String> lectureLanguages;

    private Boolean isAvailableOnly;

    @Size(max = 10)
    private String dayOfWeek; // 예: MO, TU

    @Valid
    @Size(max = 20)
    private List<@Valid ScheduleCondition> selectedSchedules;

    @Size(max = 20)
    private List<@Size(max = 10) String> credits;

    @Min(0)
    @Max(24)
    private Integer lectureHours;

    @Min(0)
    @Max(24)
    private Integer minLectureHours;

    @Size(max = 100)
    private String generalCategory;

    @Size(max = 100)
    private String generalDetail;

    private Long timetableId;

    private Boolean isWishedOnly;

    @Size(max = CourseSearchCriteria.MAX_STATUS_FILTERS)
    private List<@Size(max = CourseSearchCriteria.MAX_STATUS_LENGTH) String> statuses;

    @Size(max = CourseSearchCriteria.MAX_COURSE_DIRECTION_LENGTH)
    private String courseDirection;

    @DecimalMin("0.0")
    @DecimalMax("30.0")
    private Double minCredits;

    @Size(max = 10)
    private List<@jakarta.validation.constraints.NotNull TargetGrade> targetGrades;

    @Size(max = 20)
    private String disclosure;

    @Pattern(regexp = "name|popular|rating|current|available")
    private String sortBy = DEFAULT_SORT_BY;

    @Pattern(regexp = "asc|desc")
    private String sortOrder;

    private Long userId;

    /**
     * 강의 검색 조건을 생성하기 위한 빌더 생성자
     */
    @Builder
    public CourseSearchCondition(String name, String keyword, String professor, String subjectCode, String academicYear,
                                 String semester, List<String> classifications,
                                 String department, List<String> gradingMethods, List<String> lectureLanguages,
                                 Boolean isAvailableOnly, String dayOfWeek, List<String> credits, Integer lectureHours,
                                 Integer minLectureHours, String generalCategory, String generalDetail,
                                 List<ScheduleCondition> selectedSchedules, Long timetableId, Boolean isWishedOnly,
                                 List<String> statuses, String courseDirection, Double minCredits,
                                 List<TargetGrade> targetGrades,
                                 String disclosure,
                                 String sortBy, String sortOrder, Long userId) {
        this.name = name;
        this.keyword = keyword;
        this.professor = professor;
        this.subjectCode = subjectCode;
        this.academicYear = academicYear;
        this.semester = semester;
        this.classifications = classifications;
        this.department = department;
        this.gradingMethods = gradingMethods;
        this.lectureLanguages = lectureLanguages;
        this.isAvailableOnly = isAvailableOnly;
        this.dayOfWeek = dayOfWeek;
        this.credits = credits;
        this.lectureHours = lectureHours;
        this.minLectureHours = minLectureHours;
        this.generalCategory = generalCategory;
        this.generalDetail = generalDetail;
        this.selectedSchedules = selectedSchedules;
        this.timetableId = timetableId;
        this.isWishedOnly = isWishedOnly;
        this.statuses = statuses;
        this.courseDirection = courseDirection;
        this.minCredits = minCredits;
        this.targetGrades = targetGrades;
        this.disclosure = disclosure;
        this.sortBy = normalizeSortBy(sortBy);
        this.sortOrder = sortOrder == null ? "desc" : sortOrder;
        this.userId = userId;
    }

    public CourseSearchCriteria toCriteria(Long userId) {
        return CourseSearchCriteria.builder()
                .name(name)
                .keyword(keyword)
                .professor(professor)
                .subjectCode(subjectCode)
                .academicYear(academicYear)
                .semester(semester)
                .classifications(classifications)
                .department(department)
                .gradingMethods(gradingMethods)
                .lectureLanguages(lectureLanguages)
                .isAvailableOnly(isAvailableOnly)
                .dayOfWeek(dayOfWeek)
                .selectedSchedules(selectedSchedules == null ? null : selectedSchedules.stream()
                        .map(schedule -> CourseSelectedSchedule.builder()
                                .dayOfWeek(schedule.getDayOfWeek())
                                .startTime(schedule.getStartTime())
                                .endTime(schedule.getEndTime())
                                .build())
                        .toList())
                .credits(credits)
                .lectureHours(lectureHours)
                .minLectureHours(minLectureHours)
                .generalCategory(generalCategory)
                .generalDetail(generalDetail)
                .timetableId(timetableId)
                .isWishedOnly(isWishedOnly)
                .statuses(statuses)
                .courseDirectionTerms(CourseSearchCriteria.normalizeCourseDirectionTerms(courseDirection))
                .minCredits(minCredits)
                .targetGrades(targetGrades)
                .disclosure(disclosure)
                .sortBy(sortBy)
                .sortOrder(sortOrder == null ? "desc" : sortOrder)
                .userId(userId)
                .build();
    }

    public void setSortBy(String sortBy) {
        this.sortBy = normalizeSortBy(sortBy);
    }

    public void validateSearchValues() {
        if (hasText(keyword) && hasText(name)) {
            reject("keyword/name");
        }
        validateEnumValues(classifications, CourseClassification::from, "classifications");
        validateEnumValues(gradingMethods, GradingMethod::from, "gradingMethods");
        validateEnumValues(lectureLanguages, LectureLanguage::from, "lectureLanguages");
        validateEnumValues(statuses, CourseStatus::from, "statuses");
        statuses = normalizeTextValues(statuses);
        if (!isCourseDirectionComplexityValid()) {
            reject("courseDirection");
        }
        List<String> directionTerms = CourseSearchCriteria.normalizeCourseDirectionTerms(courseDirection);
        courseDirection = String.join(",", directionTerms);
        if (dayOfWeek != null && !dayOfWeek.isBlank() && CourseDayOfWeek.from(dayOfWeek) == null) {
            reject("dayOfWeek");
        }
        if (disclosure != null && !disclosure.isBlank() && DisclosureStatus.from(disclosure) == null) {
            reject("disclosure");
        }
        if (credits != null) {
            for (String credit : credits) {
                try {
                    double value = Double.parseDouble(credit);
                    if (value < 0 || value > 30) {
                        reject("credits");
                    }
                } catch (NumberFormatException ex) {
                    reject("credits");
                }
            }
        }
        if (minLectureHours != null && lectureHours != null && minLectureHours > lectureHours) {
            reject("lectureHours");
        }
        if (selectedSchedules != null) {
            for (ScheduleCondition schedule : selectedSchedules) {
                if (schedule == null || schedule.getDayOfWeek() == null
                        || !isValidTimeRange(schedule.getStartTime(), schedule.getEndTime())) {
                    reject("selectedSchedules");
                }
            }
        }
    }

    @AssertTrue(message = "courseDirection terms exceed the search predicate limit")
    @JsonIgnore
    public boolean isCourseDirectionComplexityValid() {
        try {
            CourseSearchCriteria.normalizeCourseDirectionTerms(courseDirection);
            return true;
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }

    private <T> void validateEnumValues(List<String> values, java.util.function.Function<String, T> mapper,
                                        String field) {
        if (values == null) {
            return;
        }
        for (String value : values) {
            if (value == null || mapper.apply(value) == null) {
                reject(field);
            }
        }
    }

    private List<String> normalizeTextValues(List<String> values) {
        if (values == null) {
            return List.of();
        }
        return values.stream().map(String::trim).distinct().toList();
    }

    private boolean isValidTimeRange(String start, String end) {
        try {
            return start != null && end != null && LocalTime.parse(start.length() == 5 ? start + ":00" : start)
                    .isBefore(LocalTime.parse(end.length() == 5 ? end + ":00" : end));
        } catch (RuntimeException ex) {
            return false;
        }
    }

    private void reject(String field) {
        throw new CustomException(ErrorCode.INVALID_INPUT, "검색 조건이 올바르지 않습니다: " + field);
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private String normalizeSortBy(String value) {
        if (value == null) {
            return DEFAULT_SORT_BY;
        }
        String normalized = value.trim();
        return WISHLIST_SORT_ALIASES.contains(normalized.toLowerCase(Locale.ROOT))
                ? DEFAULT_SORT_BY
                : normalized;
    }
}
