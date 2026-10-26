package bhoon.sugang_helper.academic.review.dto.response;

import lombok.Builder;

@Builder
public record CourseEmojiReviewResponse(String emoji, long count, boolean isMine) {
}
