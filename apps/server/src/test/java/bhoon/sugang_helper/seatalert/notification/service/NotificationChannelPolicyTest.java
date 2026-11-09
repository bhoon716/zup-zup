package bhoon.sugang_helper.seatalert.notification.service;

import static org.assertj.core.api.Assertions.assertThat;

import bhoon.sugang_helper.common.api.notifications.NotificationProperties;
import bhoon.sugang_helper.identity.api.notifications.NotificationDevice;
import bhoon.sugang_helper.identity.api.notifications.NotificationRecipient;
import bhoon.sugang_helper.seatalert.api.NotificationChannel;
import bhoon.sugang_helper.seatalert.notification.dto.provider.NotificationTarget;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class NotificationChannelPolicyTest {

    private static final String USER_EMAIL = "user@example.com";

    @Test
    @DisplayName("알림 이메일이 있으면 해당 주소를 수신 대상으로 사용한다")
    void resolveTargets_UsesNotificationEmailFallback() {
        // given
        NotificationChannelPolicy policy = new NotificationChannelPolicy(
                new NotificationProperties(true, true, true));
        NotificationRecipient user = new NotificationRecipient(1L, false, USER_EMAIL, "notify@example.com",
                false, false, false, null);

        // when
        List<NotificationTarget> targets = policy.resolveTargets(user, List.of(), NotificationChannel.EMAIL);

        // then
        assertThat(targets).hasSize(1);
        assertThat(targets.get(0).getRecipient()).isEqualTo("notify@example.com");
    }

    @Test
    @DisplayName("웹 푸시 대상에는 기기 구독 상세를 사용한다")
    void resolveTargets_UsesWebDeviceDetails() {
        // given
        NotificationChannelPolicy policy = new NotificationChannelPolicy(
                new NotificationProperties(true, true, true));
        NotificationRecipient user = new NotificationRecipient(1L, false, USER_EMAIL, null,
                false, false, false, null);
        NotificationDevice device = new NotificationDevice(1L, "endpoint", "p256", "auth");

        // when
        List<NotificationTarget> targets = policy.resolveTargets(user, List.of(device), NotificationChannel.WEB);

        // then
        assertThat(targets).hasSize(1);
        assertThat(targets.get(0).getRecipient()).isEqualTo("endpoint");
        assertThat(targets.get(0).getP256dh()).isEqualTo("p256");
        assertThat(targets.get(0).getAuth()).isEqualTo("auth");
    }

    @Test
    @DisplayName("채널 사용에는 전체 설정과 사용자 설정이 모두 필요하다")
    void isChannelEnabled_RequiresGlobalAndUserSettings() {
        // given
        NotificationChannelPolicy policy = new NotificationChannelPolicy(
                new NotificationProperties(true, false, true));
        NotificationRecipient user = new NotificationRecipient(1L, false, null, null,
                true, false, true, "discord-id");

        // when & then
        assertThat(policy.isChannelEnabled(user, NotificationChannel.EMAIL)).isTrue();
        assertThat(policy.isChannelEnabled(user, NotificationChannel.DISCORD)).isFalse();
        assertThat(policy.isChannelEnabled(user, NotificationChannel.WEB)).isFalse();
    }

    @Test
    @DisplayName("사용자 활성 채널 목록은 사용자 설정만 사용한다")
    void userEnabledChannels_UsesOnlyUserPreferences() {
        // given
        NotificationChannelPolicy policy = new NotificationChannelPolicy(
                new NotificationProperties(false, false, false));
        NotificationRecipient user = new NotificationRecipient(1L, false, USER_EMAIL, null,
                true, false, false, null);

        // when & then
        assertThat(policy.userEnabledChannels(user))
                .containsExactly(NotificationChannel.EMAIL);
    }

    @Test
    @DisplayName("탈퇴한 사용자의 알림 채널은 비활성으로 처리한다")
    void isChannelEnabled_RejectsSoftDeletedUser() {
        // given
        NotificationChannelPolicy policy = new NotificationChannelPolicy(
                new NotificationProperties(true, true, true));
        NotificationRecipient user = new NotificationRecipient(1L, true, USER_EMAIL, null,
                true, false, false, null);

        // when & then
        assertThat(policy.isChannelEnabled(user, NotificationChannel.EMAIL)).isFalse();
        assertThat(policy.resolveTargets(user, List.of(), NotificationChannel.EMAIL)).isEmpty();
    }
}
