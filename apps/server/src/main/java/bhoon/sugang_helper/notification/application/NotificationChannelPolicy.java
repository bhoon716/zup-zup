package bhoon.sugang_helper.notification.application;

import bhoon.sugang_helper.common.config.NotificationProperties;
import bhoon.sugang_helper.notification.infra.NotificationChannel;
import bhoon.sugang_helper.notification.infra.NotificationTarget;
import bhoon.sugang_helper.user.domain.DeviceType;
import bhoon.sugang_helper.user.domain.User;
import bhoon.sugang_helper.user.domain.UserDevice;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class NotificationChannelPolicy {

    private final NotificationProperties notificationProperties;

    public boolean isChannelEnabled(User user, NotificationChannel channel) {
        if (!isGlobalChannelEnabled(channel)) {
            return false;
        }

        return switch (channel) {
            case EMAIL -> user.isEmailEnabled();
            case DISCORD -> user.isDiscordEnabled() && user.getDiscordId() != null;
            case WEB -> user.isWebPushEnabled();
            case FCM -> user.isFcmEnabled();
        };
    }

    public List<NotificationTarget> resolveTargets(User user, List<UserDevice> devices, NotificationChannel channel) {
        return switch (channel) {
            case EMAIL -> List.of(NotificationTarget.of(resolveNotificationEmail(user)));
            case DISCORD ->
                    user.getDiscordId() != null ? List.of(NotificationTarget.of(user.getDiscordId())) : List.of();
            case WEB, FCM -> resolveDeviceTargets(devices, channel);
        };
    }

    public String buildNoTargetMessage(NotificationChannel channel) {
        return switch (channel) {
            case EMAIL -> "등록된 이메일 정보가 없습니다.";
            case DISCORD -> "디스코드 연동 정보가 없습니다. 먼저 디스코드를 연동해 주세요.";
            case WEB -> "등록된 웹 푸시 기기가 없습니다. 먼저 기기를 등록해 주세요.";
            case FCM -> "등록된 앱 푸시 기기가 없습니다. 먼저 기기를 등록해 주세요.";
        };
    }

    private boolean isGlobalChannelEnabled(NotificationChannel channel) {
        return switch (channel) {
            case EMAIL -> notificationProperties.email();
            case DISCORD -> notificationProperties.discord();
            case WEB -> notificationProperties.webpush();
            case FCM -> notificationProperties.fcm();
        };
    }

    private List<NotificationTarget> resolveDeviceTargets(List<UserDevice> devices, NotificationChannel channel) {
        if (devices == null) {
            return List.of();
        }

        DeviceType type = (channel == NotificationChannel.WEB) ? DeviceType.WEB : DeviceType.FCM;
        return devices.stream()
                .filter(device -> device.getType() == type)
                .map(device -> toDeviceTarget(device, channel))
                .toList();
    }

    private String resolveNotificationEmail(User user) {
        return (user.getNotificationEmail() != null && !user.getNotificationEmail().isBlank())
                ? user.getNotificationEmail()
                : user.getEmail();
    }

    private NotificationTarget toDeviceTarget(UserDevice device, NotificationChannel channel) {
        if (channel == NotificationChannel.WEB) {
            return NotificationTarget.ofWeb(device.getToken(), device.getP256dh(), device.getAuth());
        }
        return NotificationTarget.of(device.getToken());
    }
}
