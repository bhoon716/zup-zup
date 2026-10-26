package bhoon.sugang_helper.support.feedback.dto.request;

import jakarta.validation.constraints.AssertTrue;




public record AdminFeedbackAttachmentDownloadRequest(
        @AssertTrue(message = "첨부파일 열람을 확인해주세요.") boolean confirmed) {
}
