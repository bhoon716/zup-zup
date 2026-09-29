package bhoon.sugang_helper.seatalert.notification.application;

import static org.assertj.core.api.Assertions.assertThat;

import bhoon.sugang_helper.common.config.NotificationProperties;
import bhoon.sugang_helper.seatalert.contract.NotificationChannel;
import bhoon.sugang_helper.seatalert.notification.application.port.NotificationTarget;
import bhoon.sugang_helper.identity.user.contract.NotificationDevice;
import bhoon.sugang_helper.identity.user.contract.NotificationRecipient;
import java.util.List;
import org.junit.jupiter.api.Test;

class NotificationChannelPolicyTest {

    @Test
    void resolveTargets_UsesNotificationEmailFallback() {
        NotificationChannelPolicy policy = new NotificationChannelPolicy(
                new NotificationProperties(true, true, true, true));
        NotificationRecipient user = new NotificationRecipient(1L, false, "user@example.com", "notify@example.com",
                false, false, false, false, null);

        List<NotificationTarget> targets = policy.resolveTargets(user, List.of(), NotificationChannel.EMAIL);

        assertThat(targets).hasSize(1);
        assertThat(targets.get(0).getRecipient()).isEqualTo("notify@example.com");
    }

    @Test
    void resolveTargets_UsesWebDeviceDetails() {
        NotificationChannelPolicy policy = new NotificationChannelPolicy(
                new NotificationProperties(true, true, true, true));
        NotificationRecipient user = new NotificationRecipient(1L, false, "user@example.com", null,
                false, false, false, false, null);
        NotificationDevice device = new NotificationDevice(1L, NotificationDevice.Type.WEB,
                "endpoint", "p256", "auth");

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
        NotificationRecipient user = new NotificationRecipient(1L, false, null, null,
                true, false, true, true, "discord-id");

        assertThat(policy.isChannelEnabled(user, NotificationChannel.EMAIL)).isTrue();
        assertThat(policy.isChannelEnabled(user, NotificationChannel.DISCORD)).isFalse();
        assertThat(policy.isChannelEnabled(user, NotificationChannel.WEB)).isFalse();
        assertThat(policy.isChannelEnabled(user, NotificationChannel.FCM)).isTrue();
    }

    @Test
    void userEnabledChannels_UsesOnlyUserPreferences() {
        NotificationChannelPolicy policy = new NotificationChannelPolicy(
                new NotificationProperties(false, false, false, false));
        NotificationRecipient user = new NotificationRecipient(1L, false, "user@example.com", null,
                true, false, true, false, null);

        assertThat(policy.userEnabledChannels(user))
                .containsExactly(NotificationChannel.EMAIL, NotificationChannel.FCM);
    }

    @Test
    void isChannelEnabled_RejectsSoftDeletedUser() {
        NotificationChannelPolicy policy = new NotificationChannelPolicy(
                new NotificationProperties(true, true, true, true));
        NotificationRecipient user = new NotificationRecipient(1L, true, "user@example.com", null,
                true, false, false, false, null);

        assertThat(policy.isChannelEnabled(user, NotificationChannel.EMAIL)).isFalse();
        assertThat(policy.resolveTargets(user, List.of(), NotificationChannel.EMAIL)).isEmpty();
    }
}
