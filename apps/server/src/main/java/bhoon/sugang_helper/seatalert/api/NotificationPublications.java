package bhoon.sugang_helper.seatalert.api;

public interface NotificationPublications {

    void publishFeedbackReply(Long userId, Long feedbackId, Long replyId);

    void hideFeedbackReply(Long replyId);

    void hideFeedbackReplies(Long feedbackId);

    void publishAnnouncement(Long announcementId, String title);

    void setAnnouncementAvailable(Long announcementId, boolean available);
}
