package bhoon.sugang_helper.notification.domain;

import bhoon.sugang_helper.common.audit.BaseTimeEntity;
import bhoon.sugang_helper.course.domain.SeatOpenedEvent;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "seat_notification_outbox", indexes = {
        @Index(name = "idx_seat_notification_outbox_status_created", columnList = "status,created_at")
})
public class SeatNotificationOutbox extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 64)
    private String courseKey;

    @Column(nullable = false, length = 100)
    private String courseName;

    @Column(length = 50)
    private String professor;

    @Column(nullable = false)
    private Integer previousSeats;

    @Column(nullable = false)
    private Integer currentSeats;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SeatNotificationOutboxStatus status;

    @Version
    private Long version;

    @Builder
    private SeatNotificationOutbox(String courseKey, String courseName, String professor, Integer previousSeats,
                                  Integer currentSeats) {
        this.courseKey = courseKey;
        this.courseName = courseName;
        this.professor = professor;
        this.previousSeats = previousSeats;
        this.currentSeats = currentSeats;
        this.status = SeatNotificationOutboxStatus.PENDING;
    }

    public static SeatNotificationOutbox from(SeatOpenedEvent event) {
        return SeatNotificationOutbox.builder()
                .courseKey(event.courseKey())
                .courseName(event.courseName())
                .professor(event.professor())
                .previousSeats(event.previousSeats())
                .currentSeats(event.currentSeats())
                .build();
    }

    public void markProcessing() {
        this.status = SeatNotificationOutboxStatus.PROCESSING;
    }

    public void markCompleted() {
        this.status = SeatNotificationOutboxStatus.COMPLETED;
    }

    public void markDeadLettered() {
        this.status = SeatNotificationOutboxStatus.DLQ;
    }
}
