package bhoon.sugang_helper.notification.presentation;

import bhoon.sugang_helper.common.response.CommonResponse;
import bhoon.sugang_helper.notification.application.AdminNotificationDeliveryResponse;
import bhoon.sugang_helper.notification.application.AdminNotificationDeliveryService;
import bhoon.sugang_helper.user.application.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@SecurityRequirement(name = "Cookie")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin Notification Deliveries", description = "관리자용 알림 DLQ 운영 API")
public class AdminNotificationDeliveryController {

    private final AdminNotificationDeliveryService notificationDeliveryService;
    private final UserService userService;

    @Operation(summary = "DLQ delivery 목록", description = "관리자만 모든 DLQ delivery를 최신순으로 조회합니다.")
    @GetMapping("/dlq")
    public ResponseEntity<CommonResponse<Page<AdminNotificationDeliveryResponse>>> getDeadLetters(
            @PageableDefault(size = 20) Pageable pageable) {
        return CommonResponse.ok(notificationDeliveryService.getDeadLetters(pageable), "알림 DLQ 목록입니다.");
    }

    @Operation(summary = "delivery 운영 상세", description = "수신자와 원본 idempotency key를 제외한 상태만 조회합니다.")
    @GetMapping("/{deliveryId}")
    public ResponseEntity<CommonResponse<AdminNotificationDeliveryResponse>> getDelivery(
            @PathVariable Long deliveryId) {
        return CommonResponse.ok(notificationDeliveryService.getDelivery(deliveryId), "알림 delivery 상세입니다.");
    }

    @Operation(summary = "delivery 선택 재처리", description = "DLQ만 기본 재처리하며 SENT는 명시 override가 있어야 합니다.")
    @PostMapping("/{deliveryId}/replay")
    public ResponseEntity<CommonResponse<AdminNotificationDeliveryResponse>> replay(
            @PathVariable Long deliveryId,
            @RequestBody(required = false) NotificationDeliveryReplayRequest request) {
        boolean forceSentReplay = request != null && request.forceSentReplay();
        AdminNotificationDeliveryResponse response = notificationDeliveryService.replay(
                userService.getCurrentUser(), deliveryId, forceSentReplay);
        return CommonResponse.ok(response, "알림 delivery 재처리를 예약했습니다.");
    }
}
