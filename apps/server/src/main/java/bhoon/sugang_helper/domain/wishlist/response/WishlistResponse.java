package bhoon.sugang_helper.domain.wishlist.response;

import bhoon.sugang_helper.domain.course.entity.Course;
import bhoon.sugang_helper.domain.wishlist.entity.Wishlist;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Schema(description = "찜 목록 강의 응답 DTO")
public class WishlistResponse {

    @Schema(description = "찜 ID", example = "1")
    private final Long id;
    @Schema(description = "강의 키", example = "2026:10:12345:01")
    private final String courseKey;
    @Schema(description = "과목 코드", example = "12345")
    private final String subjectCode;
    @Schema(description = "분반", example = "01")
    private final String classNumber;
    @Schema(description = "강의명", example = "자료구조")
    private final String courseName;
    @Schema(description = "교수명", example = "홍길동")
    private final String professor;
    @Schema(description = "이수 구분", example = "전공필수")
    private final String classification;
    @Schema(description = "학점", example = "3")
    private final String credits;
    @Schema(description = "강의 시간", example = "월 1-A, 수 1-B")
    private final String classTime;
    @Schema(description = "현재 인원", example = "35")
    private final Integer current;
    @Schema(description = "정원", example = "40")
    private final Integer capacity;
    @Schema(description = "여석", example = "5")
    private final Integer available;
    @Schema(description = "찜 생성 시각")
    private final LocalDateTime createdAt;

    @Builder
    public WishlistResponse(Long id, String courseKey, String subjectCode, String classNumber,
                            String courseName, String professor, String classification, String credits,
                            String classTime, Integer current, Integer capacity, Integer available,
                            LocalDateTime createdAt) {
        this.id = id;
        this.courseKey = courseKey;
        this.subjectCode = subjectCode;
        this.classNumber = classNumber;
        this.courseName = courseName;
        this.professor = professor;
        this.classification = classification;
        this.credits = credits;
        this.classTime = classTime;
        this.current = current;
        this.capacity = capacity;
        this.available = available;
        this.createdAt = createdAt;
    }

    public static WishlistResponse of(Wishlist wishlist, Course course) {
        return WishlistResponse.builder()
                .id(wishlist.getId())
                .courseKey(wishlist.getCourseKey())
                .subjectCode(course.getSubjectCode())
                .classNumber(course.getClassNumber())
                .courseName(course.getName())
                .professor(course.getProfessor())
                .classification(course.getClassification() != null ? course.getClassification().getDescription() : "")
                .credits(course.getCredits())
                .classTime(course.getClassTime())
                .current(course.getCurrent())
                .capacity(course.getCapacity())
                .available(Math.max(0, course.getCapacity() - course.getCurrent()))
                .createdAt(wishlist.getCreatedAt())
                .build();
    }
}
