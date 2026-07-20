package bhoon.sugang_helper.feedback.application;

import jakarta.validation.constraints.AssertTrue;

/**
 * 관리자 첨부파일 열람의 명시적 확인 요청입니다.
 */
public record AdminFeedbackAttachmentDownloadRequest(
        @AssertTrue(message = "첨부파일 열람을 확인해주세요.") boolean confirmed) {
}
