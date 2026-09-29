package bhoon.sugang_helper.support.feedback.application;

import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;

public record FeedbackAttachmentDownload(Resource resource, String originalName, MediaType contentType) {
}
