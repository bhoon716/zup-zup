package bhoon.sugang_helper.notification.application;

import bhoon.sugang_helper.admin.application.AdminAuditService;
import bhoon.sugang_helper.common.error.CustomException;
import bhoon.sugang_helper.common.error.ErrorCode;
import bhoon.sugang_helper.notification.domain.SeatNotificationDelivery;
import bhoon.sugang_helper.notification.domain.SeatNotificationDeliveryStatus;
import bhoon.sugang_helper.notification.infra.SeatNotificationDeliveryJpaRepository;
import bhoon.sugang_helper.user.domain.User;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 관리자가 DLQ delivery를 조회하고 원인 조치 후 동일 delivery만 선택적으로 재처리합니다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminNotificationDeliveryService {

    private static final int MAX_PAGE_SIZE = 100;

    private final SeatNotificationDeliveryJpaRepository deliveryRepository;
    private final AdminAuditService adminAuditService;

    public Page<AdminNotificationDeliveryResponse> getDeadLetters(Pageable pageable) {
        return deliveryRepository.findByStatusOrderByDeadLetteredAtDescIdDesc(
                        SeatNotificationDeliveryStatus.DLQ, bounded(pageable))
                .map(AdminNotificationDeliveryResponse::from);
    }

    public AdminNotificationDeliveryResponse getDelivery(Long deliveryId) {
        return AdminNotificationDeliveryResponse.from(findDelivery(deliveryId));
    }

    @Transactional
    public AdminNotificationDeliveryResponse replay(User admin, Long deliveryId, boolean forceSentReplay) {
        SeatNotificationDelivery delivery = deliveryRepository.findByIdForUpdate(deliveryId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND));
        SeatNotificationDeliveryStatus previousStatus = delivery.getStatus();
        if (previousStatus != SeatNotificationDeliveryStatus.DLQ
                && !(previousStatus == SeatNotificationDeliveryStatus.SENT && forceSentReplay)) {
            throw new CustomException(ErrorCode.NOTIFICATION_DELIVERY_REPLAY_NOT_ALLOWED);
        }

        int attemptsBefore = delivery.getAttempts();
        delivery.replay(java.time.LocalDateTime.now());
        delivery.getOutbox().markProcessing();
        adminAuditService.recordDeliveryReplay(admin, deliveryId, previousStatus, attemptsBefore);
        return AdminNotificationDeliveryResponse.from(delivery);
    }

    private SeatNotificationDelivery findDelivery(Long deliveryId) {
        return deliveryRepository.findById(deliveryId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND));
    }

    private Pageable bounded(Pageable pageable) {
        return PageRequest.of(
                Math.max(0, pageable.getPageNumber()),
                Math.min(Math.max(1, pageable.getPageSize()), MAX_PAGE_SIZE),
                Sort.by(Sort.Order.desc("deadLetteredAt"), Sort.Order.desc("id")));
    }
}
