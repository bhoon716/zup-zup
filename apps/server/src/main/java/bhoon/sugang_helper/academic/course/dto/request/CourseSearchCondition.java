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
import bhoon.sugang_helper.common.api.error.CustomException;
import bhoon.sugang_helper.common.api.error.ErrorCode;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalTime;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.springframework.util.StringUtils;

@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PUBLIC)
@ToString
public class CourseSearchCondition {

    private static final String KEYWORD_AND_NAME_FIELD = "keyword/name";
    private static final String CLASSIFICATIONS_FIELD = "classifications";
    private static final String GRADING_METHODS_FIELD = "gradingMethods";
    private static final String LECTURE_LANGUAGES_FIELD = "lectureLanguages";
    private static final String STATUSES_FIELD = "statuses";
    private static final String COURSE_DIRECTION_FIELD = "courseDirection";
    private static final String DAY_OF_WEEK_FIELD = "dayOfWeek";
    private static final String DISCLOSURE_FIELD = "disclosure";
    private static final String LECTURE_HOURS_FIELD = "lectureHours";
    private static final String CREDITS_FIELD = "credits";
    private static final String SELECTED_SCHEDULES_FIELD = "selectedSchedules";
    private static final String DEFAULT_SORT_ORDER = "desc";

    private static final String DEFAULT_SORT_BY = CourseSearchCriteria.POPULAR_SORT;
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
    private String dayOfWeek;

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
    private List<@NotNull TargetGrade> targetGrades;

    @Size(max = 20)
    private String disclosure;

    @Pattern(regexp = "name|popular|rating|current|available")
    private String sortBy = DEFAULT_SORT_BY;

    @Pattern(regexp = "asc|desc")
    private String sortOrder;

    private Long userId;

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
        this.sortOrder = Objects.requireNonNullElse(sortOrder, DEFAULT_SORT_ORDER);
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
                .selectedSchedules(Optional.ofNullable(selectedSchedules)
                        .map(this::toSelectedSchedules)
                        .orElse(null))
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
                .sortOrder(Objects.requireNonNullElse(sortOrder, DEFAULT_SORT_ORDER))
                .userId(userId)
                .build();
    }

    private List<CourseSelectedSchedule> toSelectedSchedules(List<ScheduleCondition> schedules) {
        return schedules.stream()
                .map(this::toSelectedSchedule)
                .toList();
    }

    private CourseSelectedSchedule toSelectedSchedule(ScheduleCondition schedule) {
        return CourseSelectedSchedule.builder()
                .dayOfWeek(schedule.getDayOfWeek())
                .startTime(schedule.getStartTime())
                .endTime(schedule.getEndTime())
                .build();
    }

    public void setSortBy(String sortBy) {
        this.sortBy = normalizeSortBy(sortBy);
    }

    public void validateSearchValues() {
        if (StringUtils.hasText(keyword) && StringUtils.hasText(name)) {
            reject(KEYWORD_AND_NAME_FIELD);
        }
        validateEnumValues(classifications, CourseClassification::from, CLASSIFICATIONS_FIELD);
        validateEnumValues(gradingMethods, GradingMethod::from, GRADING_METHODS_FIELD);
        validateEnumValues(lectureLanguages, LectureLanguage::from, LECTURE_LANGUAGES_FIELD);
        validateEnumValues(statuses, CourseStatus::from, STATUSES_FIELD);
        statuses = normalizeTextValues(statuses);
        if (!isCourseDirectionComplexityValid()) {
            reject(COURSE_DIRECTION_FIELD);
        }
        List<String> directionTerms = CourseSearchCriteria.normalizeCourseDirectionTerms(courseDirection);
        courseDirection = String.join(",", directionTerms);
        if (StringUtils.hasText(dayOfWeek) && CourseDayOfWeek.from(dayOfWeek) == null) {
            reject(DAY_OF_WEEK_FIELD);
        }
        if (StringUtils.hasText(disclosure) && DisclosureStatus.from(disclosure) == null) {
            reject(DISCLOSURE_FIELD);
        }
        validateCredits();
        if (minLectureHours != null && lectureHours != null && minLectureHours > lectureHours) {
            reject(LECTURE_HOURS_FIELD);
        }
        validateSelectedSchedules();
    }

    private void validateCredits() {
        if (credits == null) {
            return;
        }
        for (String credit : credits) {
            try {
                double value = Double.parseDouble(credit);
                if (value < 0 || value > 30) {
                    reject(CREDITS_FIELD);
                }
            } catch (NumberFormatException ex) {
                reject(CREDITS_FIELD);
            }
        }
    }

    private void validateSelectedSchedules() {
        if (selectedSchedules == null) {
            return;
        }
        for (ScheduleCondition schedule : selectedSchedules) {
            if (schedule == null || schedule.getDayOfWeek() == null
                    || !isValidTimeRange(schedule.getStartTime(), schedule.getEndTime())) {
                reject(SELECTED_SCHEDULES_FIELD);
            }
        }
    }

    @AssertTrue(message = CourseSearchCriteria.COURSE_DIRECTION_LIMIT_MESSAGE)
    @JsonIgnore
    public boolean isCourseDirectionComplexityValid() {
        try {
            CourseSearchCriteria.normalizeCourseDirectionTerms(courseDirection);
            return true;
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }

    private <T> void validateEnumValues(List<String> values, Function<String, T> mapper,
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
            return start != null && end != null && parseScheduleTime(start).isBefore(parseScheduleTime(end));
        } catch (RuntimeException ex) {
            return false;
        }
    }

    private LocalTime parseScheduleTime(String value) {
        if (value.length() == 5) {
            return LocalTime.parse(value + ":00");
        }
        return LocalTime.parse(value);
    }

    private void reject(String field) {
        throw new CustomException(ErrorCode.INVALID_INPUT, "검색 조건이 올바르지 않습니다: " + field);
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
