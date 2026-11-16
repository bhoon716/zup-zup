package bhoon.sugang_helper.seatalert.notification.dto.request;

import bhoon.sugang_helper.seatalert.api.NotificationChannel;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import java.util.ArrayList;
import java.util.List;

public record OnboardingNotificationTestRequest(
        @NotBlank(message = "알림 수신 이메일은 필수입니다.")
        @Email(message = "올바른 이메일 형식이 아닙니다.")
        String notificationEmail,
        boolean emailEnabled,
        boolean webPushEnabled,
        boolean discordEnabled) {

    public List<NotificationChannel> selectedChannels() {
        List<NotificationChannel> channels = new ArrayList<>();
        if (emailEnabled) {
            channels.add(NotificationChannel.EMAIL);
        }
        if (webPushEnabled) {
            channels.add(NotificationChannel.WEB);
        }
        if (discordEnabled) {
            channels.add(NotificationChannel.DISCORD);
        }
        return channels;
    }
}
