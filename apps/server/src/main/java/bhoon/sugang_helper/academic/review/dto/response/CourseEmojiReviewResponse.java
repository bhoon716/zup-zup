package bhoon.sugang_helper.academic.review.dto.response;

import bhoon.sugang_helper.academic.review.dto.result.CourseEmojiStats;

public record CourseEmojiReviewResponse(String emoji, long count, boolean isMine) {

    public static CourseEmojiReviewResponse from(CourseEmojiStats stats) {
        return of(stats.emoji(), stats.count(), stats.currentUserReactions() > 0);
    }

    public static CourseEmojiReviewResponse of(String emoji, long count, boolean isMine) {
        return new CourseEmojiReviewResponse(emoji, count, isMine);
    }
}
