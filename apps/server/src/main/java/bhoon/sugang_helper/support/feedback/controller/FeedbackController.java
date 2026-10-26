package bhoon.sugang_helper.support.feedback.controller;

import bhoon.sugang_helper.common.web.PageableGuard;
import bhoon.sugang_helper.support.feedback.dto.result.FeedbackAttachmentDownload;
import bhoon.sugang_helper.support.feedback.dto.request.FeedbackCreateRequest;
import bhoon.sugang_helper.support.feedback.dto.response.FeedbackDetailResponse;
import bhoon.sugang_helper.support.feedback.dto.response.FeedbackResponse;
import bhoon.sugang_helper.support.feedback.service.FeedbackService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.nio.charset.StandardCharsets;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ContentDisposition;
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




@RestController
@RequestMapping("/api/v1/feedbacks")
@RequiredArgsConstructor
public class FeedbackController {

    private final FeedbackService feedbackService;




    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Long> createFeedback(
            @Valid @RequestPart("feedback") FeedbackCreateRequest request,
            @RequestPart(value = "files", required = false) List<MultipartFile> files,
            HttpServletRequest httpRequest) {

        Long feedbackId = feedbackService.createFeedbackForCurrentUser(
                request, files, resolveClientIp(httpRequest));
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(feedbackId);
    }




    @GetMapping("/me")
    public ResponseEntity<Page<FeedbackResponse>> getMyFeedbacks(
            Pageable pageable) {
        Pageable fixedPageable = PageableGuard.fixedSize(pageable);

        return ResponseEntity.ok(feedbackService.getMyFeedbacksForCurrentUser(fixedPageable));
    }




    @GetMapping("/{feedbackId}")
    public ResponseEntity<FeedbackDetailResponse> getMyFeedbackDetail(
            @PathVariable Long feedbackId) {

        return ResponseEntity.ok(feedbackService.getMyFeedbackDetailForCurrentUser(feedbackId));
    }

    @GetMapping("/{feedbackId}/attachments/{attachmentId}")
    public ResponseEntity<Resource> downloadAttachment(@PathVariable Long feedbackId, @PathVariable Long attachmentId) {
        FeedbackAttachmentDownload download = feedbackService.getAttachmentForCurrentUser(feedbackId, attachmentId);
        return ResponseEntity.ok()
                .contentType(download.contentType())
                .header("Content-Disposition", ContentDisposition.attachment()
                        .filename(download.originalName(), StandardCharsets.UTF_8)
                        .build()
                        .toString())
                .body(download.resource());
    }




    @DeleteMapping("/{feedbackId}")
    public ResponseEntity<Void> deleteFeedback(@PathVariable Long feedbackId) {

        feedbackService.deleteCurrentUsersFeedback(feedbackId);
        return ResponseEntity.noContent().build();
    }

    private String resolveClientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",", 2)[0].trim();
        }
        return request.getRemoteAddr();
    }
}
