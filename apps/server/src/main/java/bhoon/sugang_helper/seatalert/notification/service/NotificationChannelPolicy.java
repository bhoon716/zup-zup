package bhoon.sugang_helper.seatalert.notification.service;

import bhoon.sugang_helper.common.config.NotificationProperties;
import bhoon.sugang_helper.identity.user.dto.result.NotificationDevice;
import bhoon.sugang_helper.identity.user.dto.result.NotificationRecipient;
import bhoon.sugang_helper.seatalert.enums.NotificationChannel;
import bhoon.sugang_helper.seatalert.notification.dto.provider.NotificationTarget;
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
            case WEB -> resolveDeviceTargets(devices);
        };
    }

    public String buildNoTargetMessage(NotificationChannel channel) {
        return switch (channel) {
            case EMAIL -> "등록된 이메일 정보가 없습니다.";
            case DISCORD -> "디스코드 연동 정보가 없습니다. 먼저 디스코드를 연동해 주세요.";
            case WEB -> "등록된 웹 푸시 기기가 없습니다. 먼저 기기를 등록해 주세요.";
        };
    }

    private boolean isGlobalChannelEnabled(NotificationChannel channel) {
        return switch (channel) {
            case EMAIL -> notificationProperties.email();
            case DISCORD -> notificationProperties.discord();
            case WEB -> notificationProperties.webpush();
        };
    }

    private boolean isUserSettingEnabled(NotificationRecipient user, NotificationChannel channel) {
        return switch (channel) {
            case EMAIL -> user.emailEnabled();
            case DISCORD -> user.discordEnabled();
            case WEB -> user.webPushEnabled();
        };
    }

    private List<NotificationTarget> resolveDeviceTargets(List<NotificationDevice> devices) {
        if (devices == null) {
            return List.of();
        }

        return devices.stream()
                .map(device -> NotificationTarget.ofWeb(device.token(), device.p256dh(), device.auth()))
                .toList();
    }

    private String resolveNotificationEmail(NotificationRecipient user) {
        return (user.notificationEmail() != null && !user.notificationEmail().isBlank())
                ? user.notificationEmail()
                : user.email();
    }

}
