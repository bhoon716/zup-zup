package bhoon.sugang_helper.seatalert.notification.application;

import bhoon.sugang_helper.common.config.NotificationProperties;
import bhoon.sugang_helper.seatalert.contract.NotificationChannel;
import bhoon.sugang_helper.seatalert.notification.application.port.NotificationTarget;
import bhoon.sugang_helper.identity.user.contract.NotificationDevice;
import bhoon.sugang_helper.identity.user.contract.NotificationRecipient;
import java.util.Arrays;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class NotificationChannelPolicy {

    private final NotificationProperties notificationProperties;

    public boolean isChannelEnabled(NotificationRecipient user, NotificationChannel channel) {
        if (user.deleted() || !isGlobalChannelEnabled(channel)) {
            return false;
        }

        return isUserSettingEnabled(user, channel)
                && (channel != NotificationChannel.DISCORD || user.discordId() != null);
    }

    public List<NotificationChannel> userEnabledChannels(NotificationRecipient user) {
        return Arrays.stream(NotificationChannel.values())
                .filter(channel -> isUserSettingEnabled(user, channel))
                .toList();
    }

    public List<NotificationTarget> resolveTargets(NotificationRecipient user, List<NotificationDevice> devices,
                                                   NotificationChannel channel) {
        if (user.deleted()) {
            return List.of();
        }
        return switch (channel) {
            case EMAIL -> List.of(NotificationTarget.of(resolveNotificationEmail(user)));
            case DISCORD ->
                    user.discordId() != null ? List.of(NotificationTarget.of(user.discordId())) : List.of();
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

    private boolean isUserSettingEnabled(NotificationRecipient user, NotificationChannel channel) {
        return switch (channel) {
            case EMAIL -> user.emailEnabled();
            case DISCORD -> user.discordEnabled();
            case WEB -> user.webPushEnabled();
            case FCM -> user.fcmEnabled();
        };
    }

    private List<NotificationTarget> resolveDeviceTargets(List<NotificationDevice> devices,
                                                           NotificationChannel channel) {
        if (devices == null) {
            return List.of();
        }

        NotificationDevice.Type type = (channel == NotificationChannel.WEB)
                ? NotificationDevice.Type.WEB : NotificationDevice.Type.FCM;
        return devices.stream()
                .filter(device -> device.type() == type)
                .map(device -> toDeviceTarget(device, channel))
                .toList();
    }

    private String resolveNotificationEmail(NotificationRecipient user) {
        return (user.notificationEmail() != null && !user.notificationEmail().isBlank())
                ? user.notificationEmail()
                : user.email();
    }

    private NotificationTarget toDeviceTarget(NotificationDevice device, NotificationChannel channel) {
        if (channel == NotificationChannel.WEB) {
            return NotificationTarget.ofWeb(device.token(), device.p256dh(), device.auth());
        }
        return NotificationTarget.of(device.token());
    }
}
