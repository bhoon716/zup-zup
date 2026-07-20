package bhoon.sugang_helper.notification.application;

import static org.assertj.core.api.Assertions.assertThat;

import bhoon.sugang_helper.common.config.NotificationProperties;
import bhoon.sugang_helper.notification.infra.NotificationChannel;
import bhoon.sugang_helper.notification.infra.NotificationTarget;
import bhoon.sugang_helper.user.domain.DeviceType;
import bhoon.sugang_helper.user.domain.User;
import bhoon.sugang_helper.user.domain.UserDevice;
import java.util.List;
import org.junit.jupiter.api.Test;

class NotificationChannelPolicyTest {

    @Test
    void resolveTargets_UsesNotificationEmailFallback() {
        NotificationChannelPolicy policy = new NotificationChannelPolicy(
                new NotificationProperties(true, true, true, true));
        User user = User.builder()
                .email("user@example.com")
                .notificationEmail("notify@example.com")
                .build();

        List<NotificationTarget> targets = policy.resolveTargets(user, List.of(), NotificationChannel.EMAIL);

        assertThat(targets).hasSize(1);
        assertThat(targets.get(0).getRecipient()).isEqualTo("notify@example.com");
    }

    @Test
    void resolveTargets_UsesWebDeviceDetails() {
        NotificationChannelPolicy policy = new NotificationChannelPolicy(
                new NotificationProperties(true, true, true, true));
        User user = User.builder().email("user@example.com").build();
        UserDevice device = UserDevice.builder()
                .userId(1L)
                .type(DeviceType.WEB)
                .token("endpoint")
                .p256dh("p256")
                .auth("auth")
                .build();

        List<NotificationTarget> targets = policy.resolveTargets(user, List.of(device), NotificationChannel.WEB);

        assertThat(targets).hasSize(1);
        assertThat(targets.get(0).getRecipient()).isEqualTo("endpoint");
        assertThat(targets.get(0).getP256dh()).isEqualTo("p256");
        assertThat(targets.get(0).getAuth()).isEqualTo("auth");
    }

    @Test
    void isChannelEnabled_RequiresGlobalAndUserSettings() {
        NotificationChannelPolicy policy = new NotificationChannelPolicy(
                new NotificationProperties(true, false, true, true));
        User user = User.builder()
                .emailEnabled(true)
                .discordEnabled(true)
                .discordId("discord-id")
                .webPushEnabled(false)
                .fcmEnabled(true)
                .build();

        assertThat(policy.isChannelEnabled(user, NotificationChannel.EMAIL)).isTrue();
        assertThat(policy.isChannelEnabled(user, NotificationChannel.DISCORD)).isFalse();
        assertThat(policy.isChannelEnabled(user, NotificationChannel.WEB)).isFalse();
        assertThat(policy.isChannelEnabled(user, NotificationChannel.FCM)).isTrue();
    }

    @Test
    void isChannelEnabled_RejectsSoftDeletedUser() {
        NotificationChannelPolicy policy = new NotificationChannelPolicy(
                new NotificationProperties(true, true, true, true));
        User user = User.builder()
                .id(1L)
                .email("user@example.com")
                .name("사용자")
                .emailEnabled(true)
                .role(bhoon.sugang_helper.user.domain.Role.USER)
                .build();
        user.withdraw();

        assertThat(policy.isChannelEnabled(user, NotificationChannel.EMAIL)).isFalse();
        assertThat(policy.resolveTargets(user, List.of(), NotificationChannel.EMAIL)).isEmpty();
    }
}
