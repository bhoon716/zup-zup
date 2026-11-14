package bhoon.sugang_helper.seatalert.notification.controller;

import bhoon.sugang_helper.common.api.web.PageableGuard;
import bhoon.sugang_helper.seatalert.notification.dto.response.SeatInboxItemResponse;
import bhoon.sugang_helper.seatalert.notification.enums.InboxNotificationType;
import bhoon.sugang_helper.seatalert.notification.enums.SeatInboxReadFilter;
import bhoon.sugang_helper.seatalert.notification.enums.SeatInboxSort;
import bhoon.sugang_helper.seatalert.notification.enums.SeatInboxView;
import bhoon.sugang_helper.seatalert.notification.service.SeatInboxService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/notifications/inbox")
@RequiredArgsConstructor
public class SeatInboxController {

    private static final int MAX_INBOX_PAGE = Integer.MAX_VALUE / PageableGuard.PAGE_SIZE;

    private final SeatInboxService seatInboxService;

    @GetMapping
    public Page<SeatInboxItemResponse> getInbox(
            @RequestParam(defaultValue = "0") @Min(0) @Max(MAX_INBOX_PAGE) int page,
            @RequestParam(defaultValue = "INBOX") SeatInboxView view,
            @RequestParam(required = false) SeatInboxReadFilter read,
            @RequestParam(required = false) InboxNotificationType type,
            @RequestParam(required = false) String courseName,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "NEWEST") SeatInboxSort sort) {
        return seatInboxService.getInbox(view, read, type, courseName, keyword, sort, page);
    }

    @PatchMapping("/{id}/read")
    public ResponseEntity<Void> markRead(@PathVariable Long id) {
        seatInboxService.markRead(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/read-all")
    public ResponseEntity<Void> markAllRead(@RequestParam(defaultValue = "INBOX") SeatInboxView view) {
        seatInboxService.markAllRead(view);
        return ResponseEntity.noContent().build();
    }
}
