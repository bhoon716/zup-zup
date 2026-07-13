package bhoon.sugang_helper.course.application;

import bhoon.sugang_helper.course.domain.CourseSearchCriteria;
import bhoon.sugang_helper.course.domain.CourseClassification;
import bhoon.sugang_helper.course.domain.CourseDayOfWeek;
import bhoon.sugang_helper.course.domain.CourseStatus;
import bhoon.sugang_helper.course.domain.DisclosureStatus;
import bhoon.sugang_helper.course.domain.GradingMethod;
import bhoon.sugang_helper.course.domain.LectureLanguage;
import bhoon.sugang_helper.course.domain.TargetGrade;
import bhoon.sugang_helper.common.error.CustomException;
import bhoon.sugang_helper.common.error.ErrorCode;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.time.LocalTime;
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
@Schema(description = "과목 검색 조건 DTO")
public class CourseSearchCondition {
    @Schema(description = "과목명", example = "우리생활과화학")
    @Size(max = 100)
    private String name;

    @Schema(description = "교수명", example = "김혜진")
    @Size(max = 100)
    private String professor;

    @Schema(description = "과목 코드", example = "0000130844")
    @Size(max = 30)
    private String subjectCode;

    @Schema(description = "연도", example = "2026")
    @Size(max = 4)
    private String academicYear;

    @Schema(description = "학기", example = "U211600010")
    @Size(max = 30)
    private String semester;

    @Schema(description = "이수구분 리스트", example = "[\"전공선택\", \"전공필수\"]")
    @Size(max = 20)
    private List<@Size(max = 50) String> classifications;

    @Schema(description = "학과", example = "소프트웨어공학과")
    @Size(max = 100)
    private String department;

    @Schema(description = "성적평가방식 리스트", example = "[\"상대평가Ⅰ\", \"절대평가\"]")
    @Size(max = 20)
    private List<@Size(max = 50) String> gradingMethods;

    @Schema(description = "강의언어 리스트", example = "[\"한국어\", \"영어\"]")
    @Size(max = 20)
    private List<@Size(max = 50) String> lectureLanguages;

    @Schema(description = "잔여석 존재 여부", example = "true")
    private Boolean isAvailableOnly;

    @Schema(description = "요일", example = "MO")
    @Size(max = 10)
    private String dayOfWeek; // 예: MO, TU

    @Schema(description = "선택된 시간표 슬롯 리스트")
    @Valid
    @Size(max = 20)
    private List<@Valid ScheduleCondition> selectedSchedules;

    @Schema(description = "학점 리스트", example = "[\"2\", \"3\"]")
    @Size(max = 20)
    private List<@Size(max = 10) String> credits;

    @Schema(description = "강의시간(시수)", example = "3")
    @Min(0)
    @Max(24)
    private Integer lectureHours;

    @Schema(description = "최소 강의시간", example = "10")
    @Min(0)
    @Max(24)
    private Integer minLectureHours;

    @Schema(description = "교양영역구분", example = "기초")
    @Size(max = 100)
    private String generalCategory;

    @Schema(description = "교양영역상세구분", example = "공통기초")
    @Size(max = 100)
    private String generalDetail;

    @Schema(description = "연동할 시간표 ID (해당 시간표와 겹치는 강의 제외)")
    private Long timetableId;

    @Schema(description = "찜한 강의만 보기 여부", example = "true")
    private Boolean isWishedOnly;

    @Schema(description = "강의방식(설강상태) 리스트", example = "[\"일반\", \"원격\"]")
    private List<String> statuses;

    @Schema(description = "수업 운영 방향", example = "대면수업")
    private String courseDirection;

    @Schema(description = "최소 학점", example = "4")
    @DecimalMin("0.0")
    @DecimalMax("30.0")
    private Double minCredits;

    @Schema(description = "대상 학년 리스트", example = "[\"GRADE_1\", \"GRADE_2\"]")
    @Size(max = 10)
    private List<@jakarta.validation.constraints.NotNull TargetGrade> targetGrades;

    @Schema(description = "공개 여부", example = "공개")
    @Size(max = 20)
    private String disclosure;

    @Schema(description = "정렬 기준", example = "recommended")
    @Pattern(regexp = "name|popular|rating|current|available")
    private String sortBy;

    @Schema(description = "정렬 순서", example = "asc")
    @Pattern(regexp = "asc|desc")
    private String sortOrder;

    @Schema(description = "사용자 ID (내부용)", hidden = true)
    private Long userId;

    /**
     * 강의 검색 조건을 생성하기 위한 빌더 생성자
     */
    @Builder
    public CourseSearchCondition(String name, String professor, String subjectCode, String academicYear,
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
        this.sortBy = sortBy == null ? "popular" : sortBy;
        this.sortOrder = sortOrder == null ? "desc" : sortOrder;
        this.userId = userId;
    }

    public CourseSearchCriteria toCriteria(Long userId) {
        return CourseSearchCriteria.builder()
                .name(name)
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
                        .map(schedule -> CourseSearchCriteria.SelectedSchedule.builder()
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
                .courseDirection(courseDirection)
                .minCredits(minCredits)
                .targetGrades(targetGrades)
                .disclosure(disclosure)
                .sortBy(sortBy == null ? "popular" : sortBy)
                .sortOrder(sortOrder == null ? "desc" : sortOrder)
                .userId(userId)
                .build();
    }

    public void validateSearchValues() {
        validateEnumValues(classifications, CourseClassification::from, "classifications");
        validateEnumValues(gradingMethods, GradingMethod::from, "gradingMethods");
        validateEnumValues(lectureLanguages, LectureLanguage::from, "lectureLanguages");
        validateEnumValues(statuses, CourseStatus::from, "statuses");
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
}
