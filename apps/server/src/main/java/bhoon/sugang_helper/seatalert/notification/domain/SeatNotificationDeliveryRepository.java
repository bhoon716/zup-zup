package bhoon.sugang_helper.seatalert.notification.domain;

import bhoon.sugang_helper.common.domain.RepositoryContract;
import bhoon.sugang_helper.seatalert.contract.NotificationChannel;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface SeatNotificationDeliveryRepository
        extends RepositoryContract<SeatNotificationDelivery, Long> {

    boolean existsByOutboxIdAndUserIdAndChannel(Long outboxId, Long userId, NotificationChannel channel);

    long countByOutboxIdAndStatusNot(Long outboxId, SeatNotificationDeliveryStatus status);

    long countByOutboxIdAndStatus(Long outboxId, SeatNotificationDeliveryStatus status);

    long countByOutboxIdAndStatusIn(Long outboxId, List<SeatNotificationDeliveryStatus> statuses);

    Page<SeatNotificationDelivery> findByStatusOrderByDeadLetteredAtDescIdDesc(
            SeatNotificationDeliveryStatus status, Pageable pageable);

    long deleteByStatusAndDeadLetteredAtBefore(SeatNotificationDeliveryStatus status, LocalDateTime cutoff);

    Optional<SeatNotificationDelivery> findByIdForUpdate(Long id);

    Optional<SeatNotificationDelivery> findClaimedForDispatch(
            Long id, SeatNotificationDeliveryStatus status, String claimToken);

    List<SeatNotificationDelivery> findReadyForUpdate(
            SeatNotificationDeliveryStatus pending,
            SeatNotificationDeliveryStatus processing,
            LocalDateTime now,
            Pageable pageable);
}
