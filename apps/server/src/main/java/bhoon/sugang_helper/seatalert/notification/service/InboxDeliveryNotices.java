package bhoon.sugang_helper.seatalert.notification.service;

import bhoon.sugang_helper.identity.api.notifications.NotificationRecipients;
import bhoon.sugang_helper.seatalert.api.NotificationChannel;
import bhoon.sugang_helper.seatalert.notification.entity.SeatInboxItem;
import bhoon.sugang_helper.seatalert.notification.enums.InboxNotificationType;
import bhoon.sugang_helper.seatalert.notification.repository.SeatInboxRepository;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
@Transactional(propagation = Propagation.MANDATORY)
public class InboxDeliveryNotices {

    private static final int DEVICE_LABEL_LIMIT = 80;

    private final NotificationRecipients recipients;
    private final SeatInboxRepository inbox;

    public void recordFailure(Long userId, String sourceKey, String courseKey, String courseName,
                              NotificationChannel channel, String deviceLabel) {
        String channelLabel = switch (channel) {
            case EMAIL -> "이메일";
            case DISCORD -> "Discord";
            case WEB -> "웹 푸시";
        };
        String destination = StringUtils.hasText(deviceLabel)
                ? channelLabel + " (" + shortenedDeviceLabel(deviceLabel) + ")" : channelLabel;
        record(userId, InboxNotificationType.DELIVERY_FAILED, sourceKey, courseKey, courseName,
                destination + " 알림 발송 실패", "알림을 보내지 못했습니다. 알림 설정에서 수신처를 확인해 주세요.");
    }

    public void recordReregistration(Long userId, String sourceKey, String courseKey, String courseName) {
        record(userId, InboxNotificationType.DEVICE_REREGISTRATION, sourceKey, courseKey, courseName,
                "웹 푸시 기기를 등록해 주세요", "웹 푸시를 받을 수 있는 기기가 없습니다. 알림 설정에서 기기를 등록해 주세요.");
    }

    private static String shortenedDeviceLabel(String deviceLabel) {
        String label = deviceLabel.strip();
        int end = label.offsetByCodePoints(0, Math.min(label.codePointCount(0, label.length()), DEVICE_LABEL_LIMIT));
        return label.substring(0, end);
    }

    private void record(Long userId, InboxNotificationType type, String sourceKey, String courseKey,
                        String courseName, String title, String message) {
        if (recipients.findActiveByIdsForShare(List.of(userId)).isEmpty()
                || inbox.existsByUserIdAndTypeAndSourceKey(userId, type, sourceKey)) {
            return;
        }
        inbox.save(SeatInboxItem.deliveryNotice(userId, type, sourceKey, courseKey, courseName, title, message,
                LocalDateTime.now()));
    }
}
