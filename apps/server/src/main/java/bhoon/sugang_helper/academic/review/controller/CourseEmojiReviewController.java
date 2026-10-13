package bhoon.sugang_helper.academic.review.controller;

import bhoon.sugang_helper.academic.review.dto.response.CourseEmojiReviewResponse;
import bhoon.sugang_helper.academic.review.service.CourseEmojiReviewService;
import bhoon.sugang_helper.common.response.CommonResponse;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/courses")
@RequiredArgsConstructor
public class CourseEmojiReviewController {

    private final CourseEmojiReviewService emojiReviewService;

    @GetMapping("/{courseKey}/emojis")
    public ResponseEntity<CommonResponse<List<CourseEmojiReviewResponse>>> getCourseEmojiStats(
            @PathVariable String courseKey) {
        List<CourseEmojiReviewResponse> result = emojiReviewService.getCourseEmojiStats(courseKey);
        return CommonResponse.ok(result, "이모지 리뷰 통계를 조회했습니다.");
    }

    @PostMapping("/{courseKey}/emojis/toggle")
    public ResponseEntity<CommonResponse<Void>> toggleEmoji(
            @PathVariable String courseKey,
            @RequestParam String emoji) {
        emojiReviewService.toggleEmoji(courseKey, emoji);
        return CommonResponse.ok(null, "이모지 처리가 완료되었습니다.");
    }
}
