package bhoon.sugang_helper.review.application;

import lombok.Builder;

@Builder
public record CourseEmojiReviewResponse(String emoji, long count, boolean isMine) {
}
