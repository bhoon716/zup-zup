package bhoon.sugang_helper.domain.course.response;

import bhoon.sugang_helper.domain.course.entity.Course;
import bhoon.sugang_helper.domain.course.enums.TargetGrade;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Schema(description = "과목 상세 정보 응답 DTO")
public class CourseDetailResponse {

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

    @Schema(description = "대상 학년", example = "1")
    private final TargetGrade targetGrade;

    @Schema(description = "정원", example = "40")
    private final Integer capacity;

    @Schema(description = "현재 인원", example = "35")
    private final Integer current;

    @Schema(description = "여석", example = "5")
    private final Integer available;

    @Schema(description = "이수구분", example = "전공필수")
    private final String classification;

    @Schema(description = "학과", example = "컴퓨터공학부")
    private final String department;

    @Schema(description = "성적평가방식", example = "상대평가Ⅰ")
    private final String gradingMethod;

    @Schema(description = "강의언어", example = "한국어")
    private final String lectureLanguage;

    @Schema(description = "강의시간", example = "월1,2 수3")
    private final String classTime;

    @Schema(description = "학점", example = "3")
    private final String credits;

    @Schema(description = "강의시수", example = "3")
    private final Integer lectureHours;

    @Schema(description = "공개여부", example = "PUBLIC")
    private final String disclosure;

    @Schema(description = "비공개사유", example = "")
    private final String disclosureReason;

    @Schema(description = "교양영역", example = "인문소양")
    private final String generalCategory;

    @Schema(description = "교양상세", example = "")
    private final String generalDetail;

    @Schema(description = "인증구분", example = "공학인증")
    private final String accreditation;

    @Schema(description = "설강여부", example = "OPEN")
    private final String courseStatus;

    @Schema(description = "강의실", example = "7호관 101호")
    private final String classroom;

    @Schema(description = "강의계획서여부", example = "true")
    private final Boolean hasSyllabus;

    @Schema(description = "입학년도기준교양영역", example = "핵심교양")
    private final String generalCategoryByYear;

    @Schema(description = "수업운영방향", example = "대면수업")
    private final String courseDirection;

    @Schema(description = "수업시간(분)", example = "50분")
    private final String classDuration;

    @Schema(description = "상태 (AVAILABLE, FULL)", example = "AVAILABLE")
    private final String status;

    @Schema(description = "마지막 크롤링 시간", example = "2024-03-20T10:00:00")
    private final LocalDateTime lastCrawledAt;

    /**
     * 강의 엔티티를 응답 DTO로 변환
     */
    public static CourseDetailResponse from(Course course) {
        String status = course.getAvailable() > 0 ? "AVAILABLE" : "FULL";
        return CourseDetailResponse.builder()
                .courseKey(course.getCourseKey())
                .subjectCode(course.getSubjectCode())
                .name(course.getName())
                .classNumber(course.getClassNumber())
                .professor(course.getProfessor())
                .targetGrade(course.getTargetGrade())
                .capacity(course.getCapacity())
                .current(course.getCurrent())
                .available(course.getAvailable())
                .classification(course.getClassification() != null ? course.getClassification().getDescription() : null)
                .department(course.getDepartment())
                .gradingMethod(course.getGradingMethod() != null ? course.getGradingMethod().getDescription() : null)
                .lectureLanguage(
                        course.getLectureLanguage() != null ? course.getLectureLanguage().getValue() : null)
                .classTime(course.getClassTime())
                .credits(course.getCredits())
                .lectureHours(course.getLectureHours())
                .disclosure(course.getDisclosure() != null ? course.getDisclosure().getDescription() : null)
                .disclosureReason(course.getDisclosureReason())
                .generalCategory(course.getGeneralCategory())
                .generalDetail(course.getGeneralDetail())
                .accreditation(course.getAccreditation() != null ? course.getAccreditation().getDescription() : null)
                .courseStatus(course.getStatus() != null ? course.getStatus().getDescription() : null)
                .classroom(course.getClassroom())
                .hasSyllabus(course.getHasSyllabus())
                .generalCategoryByYear(course.getGeneralCategoryByYear())
                .courseDirection(course.getCourseDirection())
                .classDuration(course.getClassDuration())
                .status(status)
                .lastCrawledAt(course.getLastCrawledAt())
                .build();
    }
}
