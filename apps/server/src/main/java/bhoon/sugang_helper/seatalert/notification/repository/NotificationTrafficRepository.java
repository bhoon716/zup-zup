package bhoon.sugang_helper.seatalert.notification.repository;

import bhoon.sugang_helper.seatalert.notification.entity.NotificationHistory;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;

public interface NotificationTrafficRepository extends Repository<NotificationHistory, Long> {

    @Query(value = """
            select floor(timestampdiff(SECOND, :start, h.created_at) / :bucketSeconds) as bucket,
                   count(*) as deliveryCount
              from notification_histories h
             where h.created_at >= :start and h.created_at <= :now
             group by bucket
            """, nativeQuery = true)
    List<SeatCount> countSeatDeliveries(LocalDateTime start, LocalDateTime now, double bucketSeconds);

    @Query(value = """
            select floor(timestampdiff(SECOND, :start, d.finished_at) / :bucketSeconds) as bucket,
                   sum(case when i.type = 'FEEDBACK_REPLY' then 1 else 0 end) as feedbackReply,
                   sum(case when i.type = 'ANNOUNCEMENT' then 1 else 0 end) as announcement
              from inbox_channel_deliveries d join seat_inbox_items i on i.id = d.inbox_id
             where d.finished_at >= :start and d.finished_at <= :now
               and d.status = 'SENT' and i.type in ('FEEDBACK_REPLY', 'ANNOUNCEMENT')
             group by bucket
            """, nativeQuery = true)
    List<InboxCount> countInboxDeliveries(LocalDateTime start, LocalDateTime now, double bucketSeconds);

    long count();

    interface SeatCount {
        int getBucket();

        long getDeliveryCount();
    }

    interface InboxCount {
        int getBucket();

        long getFeedbackReply();

        long getAnnouncement();
    }
}
