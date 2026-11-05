package bhoon.sugang_helper.seatalert.notification.service;

import bhoon.sugang_helper.common.api.error.CustomException;
import bhoon.sugang_helper.common.api.error.ErrorCode;
import bhoon.sugang_helper.common.api.security.SecurityUtil;
import bhoon.sugang_helper.identity.api.notifications.NotificationRecipient;
import bhoon.sugang_helper.identity.api.notifications.NotificationRecipients;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserNotificationTestService {

    private final NotificationRecipients recipients;
    private final NotificationService notifications;

    @Transactional
    public void sendTestNotification() {
        NotificationRecipient user = recipients.findByEmail(SecurityUtil.getCurrentUserEmail())
                .filter(recipient -> !recipient.deleted())
                .orElseThrow(() -> new CustomException(ErrorCode.USER_UNAUTHORIZED));
        if (!user.emailEnabled() && !user.webPushEnabled() && !user.discordEnabled()) {
            throw new CustomException(ErrorCode.INVALID_INPUT,
                    "활성화된 알림 채널이 없습니다. 설정에서 알림을 활성화해주세요.");
        }
        notifications.sendUserTestNotification(user.id());
    }
}
