package bhoon.sugang_helper.review.presentation;

import lombok.Builder;
import lombok.Getter;

@Getter
public class CourseEmojiReviewResponse {
    private final String emoji;
    private final long count;
    private final boolean isMine;

    @Builder
    public CourseEmojiReviewResponse(String emoji, long count, boolean isMine) {
        this.emoji = emoji;
        this.count = count;
        this.isMine = isMine;
    }
}
