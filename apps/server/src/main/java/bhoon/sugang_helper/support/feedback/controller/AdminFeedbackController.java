package bhoon.sugang_helper.support.feedback.controller;

import bhoon.sugang_helper.common.web.PageableGuard;
import bhoon.sugang_helper.support.feedback.dto.request.AdminFeedbackAttachmentDownloadRequest;
import bhoon.sugang_helper.support.feedback.dto.request.FeedbackReplyCreateRequest;
import bhoon.sugang_helper.support.feedback.dto.request.FeedbackReplyUpdateRequest;
import bhoon.sugang_helper.support.feedback.dto.request.FeedbackStatusUpdateRequest;
import bhoon.sugang_helper.support.feedback.dto.response.AdminFeedbackDetailResponse;
import bhoon.sugang_helper.support.feedback.dto.response.AdminFeedbackResponse;
import bhoon.sugang_helper.support.feedback.enums.AdminFeedbackDeletionFilter;
import bhoon.sugang_helper.support.feedback.service.FeedbackService;
import jakarta.validation.Valid;
import java.nio.charset.StandardCharsets;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;




@RestController
@RequestMapping("/api/v1/admin/feedbacks")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminFeedbackController {

    private final FeedbackService feedbackService;




    @GetMapping
    public ResponseEntity<Page<AdminFeedbackResponse>> getAllFeedbacks(
            @RequestParam(required = false) String deletion,
            Pageable pageable) {
        Pageable fixedPageable = PageableGuard.fixedSize(pageable);
        return ResponseEntity.ok(feedbackService.getFeedbacksForAdmin(fixedPageable, AdminFeedbackDeletionFilter.from(deletion)));
    }




    @GetMapping("/{feedbackId}")
    public ResponseEntity<AdminFeedbackDetailResponse> getFeedbackDetail(
            @PathVariable Long feedbackId) {
        return ResponseEntity.ok(feedbackService.getFeedbackDetailForAdmin(feedbackId));
    }

    @PostMapping("/{feedbackId}/attachments/{attachmentId}/download")
    public ResponseEntity<Resource> downloadAttachment(
            @PathVariable Long feedbackId,
            @PathVariable Long attachmentId,
            @Valid @RequestBody AdminFeedbackAttachmentDownloadRequest request) {
        var download = feedbackService.getAttachmentForCurrentAdmin(
                feedbackId, attachmentId, request.confirmed());
        return ResponseEntity.ok()
                .contentType(download.contentType())
                .header("Content-Disposition", ContentDisposition.attachment()
                        .filename(download.originalName(), StandardCharsets.UTF_8)
                        .build()
                        .toString())
                .body(download.resource());
    }




    @PatchMapping("/{feedbackId}/status")
    public ResponseEntity<Void> updateFeedbackStatus(
            @PathVariable Long feedbackId,
            @Valid @RequestBody FeedbackStatusUpdateRequest request) {

        feedbackService.updateFeedbackStatusForCurrentAdmin(feedbackId, request);
        return ResponseEntity.ok().build();
    }




    @PostMapping("/{feedbackId}/reply")
    public ResponseEntity<Long> createReply(
            @PathVariable Long feedbackId,
            @Valid @RequestBody FeedbackReplyCreateRequest request) {

        Long replyId = feedbackService.createFeedbackReplyForCurrentAdmin(feedbackId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(replyId);
    }




    @PatchMapping("/reply/{replyId}")
    public ResponseEntity<Void> updateReply(
            @PathVariable Long replyId,
            @Valid @RequestBody FeedbackReplyUpdateRequest request) {

        feedbackService.updateFeedbackReplyForCurrentAdmin(replyId, request);
        return ResponseEntity.ok().build();
    }




    @DeleteMapping("/reply/{replyId}")
    public ResponseEntity<Void> deleteReply(
            @PathVariable Long replyId) {

        feedbackService.deleteFeedbackReplyForCurrentAdmin(replyId);
        return ResponseEntity.noContent().build();
    }

}
