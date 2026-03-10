package bhoon.sugang_helper.domain.feedback.controller;

import bhoon.sugang_helper.domain.feedback.dto.request.FeedbackCreateRequest;
import bhoon.sugang_helper.domain.feedback.dto.response.FeedbackDetailResponse;
import bhoon.sugang_helper.domain.feedback.dto.response.FeedbackResponse;
import bhoon.sugang_helper.domain.feedback.service.FeedbackService;
import bhoon.sugang_helper.domain.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * 일반 사용자용 피드백 API 컨트롤러입니다.
 * 건의사항 및 버그 리포트 등록, 조회, 삭제 기능을 제공합니다.
 */
@Tag(name = "Feedback", description = "건의사항 및 버그 리포트 API")
@RestController
@RequestMapping("/api/v1/feedbacks")
@RequiredArgsConstructor
public class FeedbackController {

    private final FeedbackService feedbackService;
    private final UserService userService;

    /**
     * 새로운 피드백(건의/제보)을 등록합니다.
     */
    @Operation(summary = "피드백(건의사항/버그리포트) 등록")
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Long> createFeedback(
            @Valid @RequestPart("feedback") FeedbackCreateRequest request,
            @RequestPart(value = "files", required = false) List<MultipartFile> files) {

        Long userId = getUserId();
        Long feedbackId = feedbackService.createFeedback(userId, request, files);
        return ResponseEntity.status(HttpStatus.CREATED).body(feedbackId);
    }

    /**
     * 내가 작성한 모든 피드백 목록을 조회합니다.
     */
    @Operation(summary = "내가 작성한 피드백 목록 조회")
    @GetMapping("/me")
    public ResponseEntity<Page<FeedbackResponse>> getMyFeedbacks(
            @PageableDefault(size = 10) Pageable pageable) {

        Long userId = getUserId();
        return ResponseEntity.ok(feedbackService.getMyFeedbacks(userId, pageable));
    }

    /**
     * 내가 작성한 특정 피드백의 상세 내용을 조회합니다.
     */
    @Operation(summary = "내가 작성한 피드백 상세 조회")
    @GetMapping("/{feedbackId}")
    public ResponseEntity<FeedbackDetailResponse> getMyFeedbackDetail(
            @PathVariable Long feedbackId) {

        Long userId = getUserId();
        return ResponseEntity.ok(feedbackService.getMyFeedbackDetail(userId, feedbackId));
    }

    /**
     * 작성한 피드백을 삭제합니다 (소프트 삭제 적용).
     */
    @Operation(summary = "피드백 삭제 (소프트 삭제)")
    @DeleteMapping("/{feedbackId}")
    public ResponseEntity<Void> deleteFeedback(@PathVariable Long feedbackId) {

        Long userId = getUserId();
        feedbackService.deleteFeedback(userId, feedbackId);
        return ResponseEntity.noContent().build();
    }

    /**
     * 현재 인증된 유저의 ID를 조회합니다.
     */
    private Long getUserId() {
        return userService.getCurrentUser().getId();
    }
}
