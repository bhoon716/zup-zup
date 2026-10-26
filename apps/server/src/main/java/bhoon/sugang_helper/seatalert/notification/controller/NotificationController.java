package bhoon.sugang_helper.seatalert.notification.controller;

import bhoon.sugang_helper.common.web.PageableGuard;
import bhoon.sugang_helper.seatalert.notification.dto.response.NotificationHistoryResponse;
import bhoon.sugang_helper.seatalert.notification.service.NotificationHistoryQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationHistoryQueryService notificationHistoryQuery;




    @GetMapping("/history")
    public ResponseEntity<Slice<NotificationHistoryResponse>> getMyNotificationHistory(
            Pageable pageable) {
        Pageable fixedPageable = PageableGuard.fixedSize(pageable);
        Slice<NotificationHistoryResponse> histories = notificationHistoryQuery
                .findForCurrentUser(fixedPageable)
                .map(NotificationHistoryResponse::from);
        return ResponseEntity.ok(histories);
    }
}
