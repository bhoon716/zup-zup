package bhoon.sugang_helper.seatalert.notification.controller;

import bhoon.sugang_helper.common.web.PageableGuard;
import bhoon.sugang_helper.seatalert.notification.dto.response.NotificationHistoryResponse;
import bhoon.sugang_helper.seatalert.notification.service.NotificationHistoryQuery;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private static final int MAX_HISTORY_PAGE_SIZE = 100;

    private final NotificationHistoryQuery notificationHistoryQuery;

    /**
     * 현재 로그인한 사용자의 전체 알림 수신 내역을 조회합니다.
     */
    @GetMapping("/history")
    public ResponseEntity<Slice<NotificationHistoryResponse>> getMyNotificationHistory(
            @PageableDefault(size = 30) Pageable pageable) {
        Pageable boundedPageable = PageableGuard.requireBounded(pageable, MAX_HISTORY_PAGE_SIZE, 10_000);
        Slice<NotificationHistoryResponse> histories = notificationHistoryQuery
                .findForCurrentUser(boundedPageable)
                .map(NotificationHistoryResponse::from);
        return ResponseEntity.ok(histories);
    }
}
