package bhoon.sugang_helper.support.feedback.dto.result;

import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;

public record FeedbackAttachmentDownload(Resource resource, String originalName, MediaType contentType) {
}
