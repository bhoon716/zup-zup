package bhoon.sugang_helper.domain.feedback.controller;

import bhoon.sugang_helper.domain.feedback.dto.request.FeedbackReplyCreateRequest;
import bhoon.sugang_helper.domain.feedback.dto.request.FeedbackReplyUpdateRequest;
import bhoon.sugang_helper.domain.feedback.dto.request.FeedbackStatusUpdateRequest;
import bhoon.sugang_helper.domain.feedback.dto.response.FeedbackDetailResponse;
import bhoon.sugang_helper.domain.feedback.dto.response.FeedbackResponse;
import bhoon.sugang_helper.domain.feedback.service.FeedbackService;
import bhoon.sugang_helper.domain.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 관리자용 피드백 관리 API 컨트롤러입니다.
 * 전체 피드백 조회, 상태 변경, 답변 작성 기능을 제공하며 관리자 권한(ROLE_ADMIN)이 필요합니다.
 */
@Tag(name = "Admin Feedback", description = "관리자용 피드백 관리 API")
@RestController
@RequestMapping("/api/v1/admin/feedbacks")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminFeedbackController {

    private final FeedbackService feedbackService;
    private final UserService userService;

    /**
     * 서비스 전체 피드백 목록을 조회합니다.
     */
    @Operation(summary = "전체 피드백 목록 조회 (관리자)")
    @GetMapping
    public ResponseEntity<Page<FeedbackResponse>> getAllFeedbacks(
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(feedbackService.getFeedbacksForAdmin(pageable));
    }

    /**
     * 특정 피드백의 상세 내용을 조회합니다 (사용 데이터 포함).
     */
    @Operation(summary = "피드백 상세 조회 (관리자)")
    @GetMapping("/{feedbackId}")
    public ResponseEntity<FeedbackDetailResponse> getFeedbackDetail(
            @PathVariable Long feedbackId) {
        return ResponseEntity.ok(feedbackService.getFeedbackDetailForAdmin(feedbackId));
    }

    /**
     * 피드백의 처리 상태(대기, 진행중, 완료, 반려)를 변경합니다.
     */
    @Operation(summary = "피드백 상태 변경 (관리자 수동)")
    @PatchMapping("/{feedbackId}/status")
    public ResponseEntity<Void> updateFeedbackStatus(
            @PathVariable Long feedbackId,
            @Valid @RequestBody FeedbackStatusUpdateRequest request) {

        Long adminId = getAdminId();
        feedbackService.updateFeedbackStatus(adminId, feedbackId, request);
        return ResponseEntity.ok().build();
    }

    /**
     * 관리자 답변을 등록합니다.
     */
    @Operation(summary = "피드백 답변 작성 (관리자)")
    @PostMapping("/{feedbackId}/reply")
    public ResponseEntity<Long> createReply(
            @PathVariable Long feedbackId,
            @Valid @RequestBody FeedbackReplyCreateRequest request) {

        Long adminId = getAdminId();
        Long replyId = feedbackService.createFeedbackReply(adminId, feedbackId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(replyId);
    }

    /**
     * 오타 또는 내용 보충을 위해 등록된 답변을 수정합니다.
     */
    @Operation(summary = "피드백 답변 수정 (관리자)")
    @PatchMapping("/reply/{replyId}")
    public ResponseEntity<Void> updateReply(
            @PathVariable Long replyId,
            @Valid @RequestBody FeedbackReplyUpdateRequest request) {

        Long adminId = getAdminId();
        feedbackService.updateFeedbackReply(adminId, replyId, request);
        return ResponseEntity.ok().build();
    }

    /**
     * 현재 인증된 관리자의 유저 ID를 조회합니다.
     */
    private Long getAdminId() {
        return userService.getCurrentUser().getId();
    }
}
