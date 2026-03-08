package bhoon.sugang_helper.domain.course.response;

import bhoon.sugang_helper.domain.course.entity.Course;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Schema(description = "과목 정보 응답 DTO (목록용)")
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

    @Schema(description = "여석", example = "5")
    private final Integer available;

    @Schema(description = "이수구분", example = "전공필수")
    private final String classification;

    @Schema(description = "강의시간", example = "월1,2 수3")
    private final String classTime;

    @Schema(description = "학점", example = "3")
    private final String credits;

    @Schema(description = "강의실", example = "7호관 101호")
    private final String classroom;

    @Schema(description = "상태 (AVAILABLE, FULL)", example = "AVAILABLE")
    private final String status;

    @Schema(description = "구독 가능 여부 (현재 추적 중인 학기 여부)", example = "true")
    private final Boolean isSubscribable;

    @Schema(description = "평균 별점", example = "4.5")
    private final Float averageRating;

    @Schema(description = "리뷰 수", example = "10")
    private final Integer reviewCount;

    @Schema(description = "마지막 크롤링 시간", example = "2024-03-20T10:00:00")
    private final LocalDateTime lastCrawledAt;

    /**
     * 강의 엔티티를 클라이언트 응답용 DTO로 변환합니다. (목록용)
     */
    public static CourseResponse from(Course course, String currentYear, String currentSemester) {
        // 여석 유무에 따른 상태 결정
        String status = course.getAvailable() > 0 ? "AVAILABLE" : "FULL";
        // 현재 추적 중인 학기인지 확인
        boolean isSubscribable = course.isMatchingTarget(currentYear, currentSemester);

        return CourseResponse.builder()
                .courseKey(course.getCourseKey())
                .subjectCode(course.getSubjectCode())
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
                .lastCrawledAt(course.getLastCrawledAt())
                .isSubscribable(isSubscribable)
                .build();
    }
}
