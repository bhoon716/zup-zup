package bhoon.sugang_helper.support.feedback.dto.response;

public record AdminFeedbackAttachmentResponse(Long id) {

    public static AdminFeedbackAttachmentResponse of(Long id) {
        return new AdminFeedbackAttachmentResponse(id);
    }
}
