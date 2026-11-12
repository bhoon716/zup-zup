package bhoon.sugang_helper.seatalert.notification.entity;

import bhoon.sugang_helper.seatalert.notification.enums.InboxNotificationType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "seat_inbox_items", uniqueConstraints = {
    @UniqueConstraint(name = "uk_seat_inbox_outbox_user", columnNames = {"outbox_id", "user_id"}),
    @UniqueConstraint(name = "uk_inbox_user_type_source", columnNames = {"user_id", "type", "source_key"})
})
public class SeatInboxItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(updatable = false)
    private Long outboxId;

    @Column(nullable = false, updatable = false)
    private Long userId;

    @Column(updatable = false, length = 64)
    private String courseKey;

    @Column(updatable = false, length = 100)
    private String courseName;

    @Column(updatable = false)
    private Integer availableSeats;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false, length = 30)
    private InboxNotificationType type;

    @Column(updatable = false, length = 128)
    private String sourceKey;

    @Column(updatable = false)
    private Long targetId;

    @Column(updatable = false)
    private String title;

    @Column(updatable = false, length = 500)
    private String message;

    @Column(nullable = false)
    private boolean available = true;

    @Column(nullable = false, updatable = false)
    private LocalDateTime occurredAt;

    private LocalDateTime readAt;

    private LocalDateTime deletedAt;

    private SeatInboxItem(SeatNotificationOutbox outbox, Long userId) {
        this.outboxId = outbox.getId();
        this.userId = userId;
        this.courseKey = outbox.getCourseKey();
        this.courseName = outbox.getCourseName();
        this.availableSeats = outbox.getCurrentSeats();
        this.type = InboxNotificationType.SEAT_OPENED;
        this.occurredAt = outbox.getCreatedAt();
    }

    public static SeatInboxItem from(SeatNotificationOutbox outbox, Long userId) {
        return new SeatInboxItem(outbox, userId);
    }

    public static SeatInboxItem feedbackReply(Long userId, Long feedbackId, Long replyId, LocalDateTime now) {
        SeatInboxItem item = notice(userId, InboxNotificationType.FEEDBACK_REPLY, replyId.toString(), now);
        item.targetId = feedbackId;
        item.title = "문의에 답변이 등록되었습니다";
        item.message = "줍줍에서 문의 답변을 확인해 주세요.";
        return item;
    }

    public static SeatInboxItem announcement(Long userId, Long announcementId, String title, LocalDateTime now) {
        SeatInboxItem item = notice(userId, InboxNotificationType.ANNOUNCEMENT, announcementId.toString(), now);
        item.targetId = announcementId;
        item.title = title;
        item.message = "줍줍에서 중요 공지 내용을 확인해 주세요.";
        return item;
    }

    public static SeatInboxItem deliveryNotice(Long userId, InboxNotificationType type, String sourceKey,
                                                String courseKey, String courseName, String title, String message,
                                                LocalDateTime now) {
        SeatInboxItem item = notice(userId, type, sourceKey, now);
        item.courseKey = courseKey;
        item.courseName = courseName;
        item.title = title;
        item.message = message;
        return item;
    }

    private static SeatInboxItem notice(Long userId, InboxNotificationType type, String sourceKey, LocalDateTime now) {
        SeatInboxItem item = new SeatInboxItem();
        item.userId = userId;
        item.type = type;
        item.sourceKey = sourceKey;
        item.occurredAt = now;
        return item;
    }
}
