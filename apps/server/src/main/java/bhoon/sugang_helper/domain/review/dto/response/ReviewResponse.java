package bhoon.sugang_helper.domain.review.dto.response;

import bhoon.sugang_helper.domain.review.entity.CourseReview;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "강의 리뷰 응답 정보 (익명 보장)")
public record ReviewResponse(
        @Schema(description = "리뷰 ID", example = "1") Long id,

        @Schema(description = "강의 코드", example = "2026:U12345:0001:1") String courseKey,

        @Schema(description = "별점 (1~5)", example = "5") int rating,

        @Schema(description = "리뷰 내용", example = "정말 유익한 강의였습니다!") String content,

        @Schema(description = "공감 수", example = "12") int likeCount,

        @Schema(description = "비공감 수", example = "1") int dislikeCount,

        @Schema(description = "본인이 작성한 글인지 여부", example = "true") boolean isMine,

        @Schema(description = "작성일시") LocalDateTime createdAt,

        @Schema(description = "수정일시") LocalDateTime updatedAt) {
    public static ReviewResponse of(CourseReview review, Long currentUserId) {
        return new ReviewResponse(
                review.getId(),
                review.getCourseKey(),
                review.getRating(),
                review.getContent(),
                review.getLikeCount(),
                review.getDislikeCount(),
                review.getUserId().equals(currentUserId),
                review.getCreatedAt(),
                review.getUpdatedAt());
    }
}
