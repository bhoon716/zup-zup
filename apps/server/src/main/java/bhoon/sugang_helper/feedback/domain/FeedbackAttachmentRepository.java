package bhoon.sugang_helper.feedback.domain;

import bhoon.sugang_helper.common.domain.RepositoryContract;
import java.util.List;
import java.util.Optional;

public interface FeedbackAttachmentRepository extends RepositoryContract<FeedbackAttachment, Long> {
    Optional<FeedbackAttachment> findByIdAndFeedbackId(Long attachmentId, Long feedbackId);

    List<FeedbackAttachment> findAllByFeedbackId(Long feedbackId);

    List<FeedbackAttachment> findAllByFeedbackUserId(Long userId);
}
