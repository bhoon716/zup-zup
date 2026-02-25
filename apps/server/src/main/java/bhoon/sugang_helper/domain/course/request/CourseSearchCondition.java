package bhoon.sugang_helper.domain.course.request;

import bhoon.sugang_helper.domain.course.enums.TargetGrade;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
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
    private String name;

    @Schema(description = "교수명", example = "김혜진")
    private String professor;

    @Schema(description = "과목 코드", example = "0000130844")
    private String subjectCode;

    @Schema(description = "연도", example = "2026")
    private String academicYear;

    @Schema(description = "학기", example = "U211600010")
    private String semester;

    @Schema(description = "이수구분", example = "전공선택")
    private String classification;

    @Schema(description = "학과", example = "소프트웨어공학과")
    private String department;

    @Schema(description = "성적평가방식", example = "상대평가Ⅰ")
    private String gradingMethod;

    @Schema(description = "강의언어", example = "한국어")
    private String lectureLanguage;

    @Schema(description = "잔여석 존재 여부", example = "true")
    private Boolean isAvailableOnly;

    @Schema(description = "요일", example = "MO")
    private String dayOfWeek; // 예: MO, TU

    @Schema(description = "선택된 시간표 슬롯 리스트")
    private List<ScheduleCondition> selectedSchedules;

    @Schema(description = "학점", example = "3")
    private String credits;

    @Schema(description = "강의시간(시수)", example = "3")
    private Integer lectureHours;

    @Schema(description = "최소 강의시간", example = "10")
    private Integer minLectureHours;

    @Schema(description = "교양영역구분", example = "기초")
    private String generalCategory;

    @Schema(description = "교양영역상세구분", example = "공통기초")
    private String generalDetail;

    @Schema(description = "연동할 시간표 ID (해당 시간표와 겹치는 강의 제외)")
    private Long timetableId;

    @Schema(description = "찜한 강의만 보기 여부", example = "true")
    private Boolean isWishedOnly;

    @Schema(description = "강의방식(설강상태)", example = "일반")
    private String status;

    @Schema(description = "수업 운영 방향", example = "대면수업")
    private String courseDirection;

    @Schema(description = "최소 학점", example = "4")
    private Double minCredits;

    @Schema(description = "대상 학년", example = "1")
    private TargetGrade targetGrade;

    @Schema(description = "공개 여부", example = "공개")
    private String disclosure;

    @Schema(description = "정렬 기준", example = "recommended")
    private String sortBy;

    @Schema(description = "정렬 순서", example = "asc")
    private String sortOrder;

    @Schema(description = "사용자 ID (내부용)", hidden = true)
    private Long userId;

    /**
     * 강의 검색 조건을 생성하기 위한 빌더 생성자
     */
    @Builder
    public CourseSearchCondition(String name, String professor, String subjectCode, String academicYear,
            String semester, String classification,
            String department, String gradingMethod, String lectureLanguage,
            Boolean isAvailableOnly, String dayOfWeek, String credits, Integer lectureHours,
            Integer minLectureHours, String generalCategory, String generalDetail,
            List<ScheduleCondition> selectedSchedules, Long timetableId, Boolean isWishedOnly,
            String status, String courseDirection, Double minCredits, TargetGrade targetGrade, String disclosure,
            String sortBy, String sortOrder, Long userId) {
        this.name = name;
        this.professor = professor;
        this.subjectCode = subjectCode;
        this.academicYear = academicYear;
        this.semester = semester;
        this.classification = classification;
        this.department = department;
        this.gradingMethod = gradingMethod;
        this.lectureLanguage = lectureLanguage;
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
        this.status = status;
        this.courseDirection = courseDirection;
        this.minCredits = minCredits;
        this.targetGrade = targetGrade;
        this.disclosure = disclosure;
        this.sortBy = sortBy;
        this.sortOrder = sortOrder;
        this.userId = userId;
    }
}
