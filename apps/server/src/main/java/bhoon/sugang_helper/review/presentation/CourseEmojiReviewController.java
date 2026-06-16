package bhoon.sugang_helper.review.presentation;

import bhoon.sugang_helper.common.response.CommonResponse;
import bhoon.sugang_helper.review.application.CourseEmojiReviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/courses")
@RequiredArgsConstructor
@Tag(name = "Course Emoji Review", description = "강의 이모지 리뷰 API (시스템 이모지 전반 지원)")
public class CourseEmojiReviewController {

    private final CourseEmojiReviewService emojiReviewService;

    @Operation(summary = "이모지 리뷰 통계 조회", description = "강의에 달린 이모지의 카운트 및 본인 탭 여부를 반환합니다.")
    @GetMapping("/{courseKey}/emojis")
    public ResponseEntity<CommonResponse<List<CourseEmojiReviewResponse>>> getCourseEmojiStats(
            @PathVariable String courseKey) {
        List<CourseEmojiReviewResponse> result = emojiReviewService.getCourseEmojiStats(courseKey);
        return CommonResponse.ok(result, "이모지 리뷰 통계를 조회했습니다.");
    }

    @Operation(summary = "이모지 리뷰 토글", description = "선택한 이모지를 토글합니다. 처음 탭하면 추가, 재탭하면 취소.")
    @PostMapping("/{courseKey}/emojis/toggle")
    public ResponseEntity<CommonResponse<Void>> toggleEmoji(
            @PathVariable String courseKey,
            @RequestParam String emoji) {
        emojiReviewService.toggleEmoji(courseKey, emoji);
        return CommonResponse.ok(null, "이모지 처리가 완료되었습니다.");
    }
}
