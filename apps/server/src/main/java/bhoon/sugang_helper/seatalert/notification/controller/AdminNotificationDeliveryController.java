package bhoon.sugang_helper.seatalert.notification.controller;

import bhoon.sugang_helper.common.web.PageableGuard;
import bhoon.sugang_helper.seatalert.notification.dto.request.NotificationDeliveryReplayRequest;
import bhoon.sugang_helper.seatalert.notification.dto.response.AdminNotificationDeliveryResponse;
import bhoon.sugang_helper.seatalert.notification.service.AdminNotificationDeliveryService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 단일 관리자도 전체 DLQ를 안전한 운영 정보만으로 조회하고 선택 재처리할 수 있는 API입니다.
 */
@RestController
@RequestMapping("/api/v1/admin/notification-deliveries")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminNotificationDeliveryController {

    private final AdminNotificationDeliveryService notificationDeliveryService;

    @GetMapping("/dlq")
    public ResponseEntity<Page<AdminNotificationDeliveryResponse>> getDeadLetters(
            Pageable pageable) {
        Pageable fixedPageable = PageableGuard.fixedSize(pageable);
        return ResponseEntity.ok(notificationDeliveryService.getDeadLetters(fixedPageable));
    }

    @GetMapping("/{deliveryId}")
    public ResponseEntity<AdminNotificationDeliveryResponse> getDelivery(
            @PathVariable Long deliveryId) {
        return ResponseEntity.ok(notificationDeliveryService.getDelivery(deliveryId));
    }

    @PostMapping("/{deliveryId}/replay")
    public ResponseEntity<AdminNotificationDeliveryResponse> replay(
            @PathVariable Long deliveryId,
            @RequestBody(required = false) NotificationDeliveryReplayRequest request) {
        boolean forceSentReplay = request != null && request.forceSentReplay();
        AdminNotificationDeliveryResponse response = notificationDeliveryService.replay(
                notificationDeliveryService.getCurrentAdminId(), deliveryId, forceSentReplay);
        return ResponseEntity.ok(response);
    }
}
