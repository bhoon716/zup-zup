package bhoon.sugang_helper.seatalert.notification.controller;

import bhoon.sugang_helper.common.response.CommonResponse;
import bhoon.sugang_helper.seatalert.notification.dto.request.NotificationDeliveryReplayRequest;
import bhoon.sugang_helper.seatalert.notification.dto.response.AdminNotificationDeliveryResponse;
import bhoon.sugang_helper.seatalert.notification.service.AdminNotificationDeliveryService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
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
    public ResponseEntity<CommonResponse<Page<AdminNotificationDeliveryResponse>>> getDeadLetters(
            @PageableDefault(size = 20) Pageable pageable) {
        return CommonResponse.ok(notificationDeliveryService.getDeadLetters(pageable), "알림 DLQ 목록입니다.");
    }

    @GetMapping("/{deliveryId}")
    public ResponseEntity<CommonResponse<AdminNotificationDeliveryResponse>> getDelivery(
            @PathVariable Long deliveryId) {
        return CommonResponse.ok(notificationDeliveryService.getDelivery(deliveryId), "알림 delivery 상세입니다.");
    }

    @PostMapping("/{deliveryId}/replay")
    public ResponseEntity<CommonResponse<AdminNotificationDeliveryResponse>> replay(
            @PathVariable Long deliveryId,
            @RequestBody(required = false) NotificationDeliveryReplayRequest request) {
        boolean forceSentReplay = request != null && request.forceSentReplay();
        AdminNotificationDeliveryResponse response = notificationDeliveryService.replay(
                notificationDeliveryService.getCurrentAdminId(), deliveryId, forceSentReplay);
        return CommonResponse.ok(response, "알림 delivery 재처리를 예약했습니다.");
    }
}
