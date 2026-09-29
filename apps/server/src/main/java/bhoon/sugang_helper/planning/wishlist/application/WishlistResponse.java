package bhoon.sugang_helper.planning.wishlist.application;

import bhoon.sugang_helper.academic.course.contract.CourseSummary;
import bhoon.sugang_helper.planning.wishlist.domain.Wishlist;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import lombok.Builder;

@Schema(description = "찜 목록 강의 응답 DTO")
@Builder
public record WishlistResponse(
    @Schema(description = "찜 ID", example = "1") Long id,
    @Schema(description = "강의 키", example = "2026:10:12345:01") String courseKey,
    @Schema(description = "과목 코드", example = "12345") String subjectCode,
    @Schema(description = "분반", example = "01") String classNumber,
    @Schema(description = "강의명", example = "자료구조") String courseName,
    @Schema(description = "교수명", example = "홍길동") String professor,
    @Schema(description = "이수 구분", example = "전공필수") String classification,
    @Schema(description = "학점", example = "3") String credits,
    @Schema(description = "강의 시간", example = "월 1-A, 수 1-B") String classTime,
    @Schema(description = "현재 인원", example = "35") Integer current,
    @Schema(description = "정원", example = "40") Integer capacity,
    @Schema(description = "여석", example = "5") Integer available,
    @Schema(description = "찜 생성 시각") LocalDateTime createdAt
) {
    public static WishlistResponse of(Wishlist wishlist, CourseSummary course) {
        return WishlistResponse.builder()
                .id(wishlist.getId())
                .courseKey(wishlist.getCourseKey())
                .subjectCode(course.subjectCode())
                .classNumber(course.classNumber())
                .courseName(course.name())
                .professor(course.professor())
                .classification(course.classification() != null ? course.classification() : "")
                .credits(course.credits())
                .classTime(course.classTime())
                .current(course.current())
                .capacity(course.capacity())
                .available(course.available())
                .createdAt(wishlist.getCreatedAt())
                .build();
    }
}
