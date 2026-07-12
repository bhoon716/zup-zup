package bhoon.sugang_helper.feedback.domain;

import bhoon.sugang_helper.common.domain.RepositoryContract;
import java.util.List;

public interface FeedbackAttachmentRepository extends RepositoryContract<FeedbackAttachment, Long> {
    List<FeedbackAttachment> findAllByFeedbackId(Long feedbackId);

    List<FeedbackAttachment> findAllByFeedbackUserId(Long userId);
}
