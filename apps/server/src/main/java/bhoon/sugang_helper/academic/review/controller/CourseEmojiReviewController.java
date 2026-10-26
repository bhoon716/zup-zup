package bhoon.sugang_helper.academic.review.controller;

import bhoon.sugang_helper.academic.review.dto.response.CourseEmojiReviewResponse;
import bhoon.sugang_helper.academic.review.service.CourseEmojiReviewService;
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
    public ResponseEntity<List<CourseEmojiReviewResponse>> getCourseEmojiStats(
            @PathVariable String courseKey) {
        List<CourseEmojiReviewResponse> result = emojiReviewService.getCourseEmojiStats(courseKey);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/{courseKey}/emojis/toggle")
    public ResponseEntity<Void> toggleEmoji(
            @PathVariable String courseKey,
            @RequestParam String emoji) {
        emojiReviewService.toggleEmoji(courseKey, emoji);
        return ResponseEntity.ok(null);
    }
}
