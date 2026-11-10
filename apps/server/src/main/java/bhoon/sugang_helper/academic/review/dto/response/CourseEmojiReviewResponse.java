package bhoon.sugang_helper.academic.review.dto.response;

public record CourseEmojiReviewResponse(String emoji, long count, boolean isMine) {

    public static CourseEmojiReviewResponse of(String emoji, long count, boolean isMine) {
        return new CourseEmojiReviewResponse(emoji, count, isMine);
    }
}
