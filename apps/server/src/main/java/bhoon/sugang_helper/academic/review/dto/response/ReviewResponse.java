package bhoon.sugang_helper.academic.review.dto.response;

import bhoon.sugang_helper.academic.review.entity.CourseReview;
import java.time.LocalDateTime;

public record ReviewResponse(
        Long id,

        String courseKey,

        int rating,

        int likeCount,

        int dislikeCount,

        boolean isMine,

        LocalDateTime createdAt,

        LocalDateTime updatedAt) {
    public static ReviewResponse of(CourseReview review, Long currentUserId) {
        return new ReviewResponse(
                review.getId(),
                review.getCourseKey(),
                review.getRating(),
                review.getLikeCount(),
                review.getDislikeCount(),
                review.getUserId().equals(currentUserId),
                review.getCreatedAt(),
                review.getUpdatedAt());
    }
}
