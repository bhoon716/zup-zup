package bhoon.sugang_helper.feedback.application;

import org.springframework.core.io.Resource;

public record FeedbackAttachmentDownload(Resource resource, String originalName) {
}
