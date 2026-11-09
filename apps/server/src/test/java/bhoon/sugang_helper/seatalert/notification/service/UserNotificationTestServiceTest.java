package bhoon.sugang_helper.seatalert.notification.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import bhoon.sugang_helper.common.api.error.CustomException;
import bhoon.sugang_helper.common.api.error.ErrorCode;
import bhoon.sugang_helper.common.api.security.SecurityUtil;
import bhoon.sugang_helper.identity.api.notifications.NotificationRecipient;
import bhoon.sugang_helper.identity.api.notifications.NotificationRecipients;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UserNotificationTestServiceTest {

    private static final String EMAIL = "module@example.invalid";

    @Mock private NotificationRecipients recipients;

    @Mock private NotificationService notifications;

    @InjectMocks private UserNotificationTestService service;

    @Test
    @DisplayName("인증된 활성 수신자에게 테스트 알림을 요청한다")
    void sendsForTheAuthenticatedActiveRecipient() {
        // given
        given(recipients.findByEmail(EMAIL)).willReturn(Optional.of(recipient(false, true)));

        // when & then
        try (var security = mockStatic(SecurityUtil.class)) {
            security.when(SecurityUtil::getCurrentUserEmail).thenReturn(EMAIL);

            service.sendTestNotification();

            verify(notifications).sendUserTestNotification(1L);
        }
    }

    @Test
    @DisplayName("활성 채널이 없으면 발송 전에 거부한다")
    void rejectsDisabledChannelsBeforeSending() {
        // given
        given(recipients.findByEmail(EMAIL)).willReturn(Optional.of(recipient(false, false)));

        // when & then
        try (var security = mockStatic(SecurityUtil.class)) {
            security.when(SecurityUtil::getCurrentUserEmail).thenReturn(EMAIL);

            assertThatThrownBy(service::sendTestNotification).isInstanceOfSatisfying(CustomException.class,
                    exception -> assertThat(exception.getErrorCode())
                            .isEqualTo(ErrorCode.INVALID_INPUT));
            verifyNoInteractions(notifications);
        }
    }

    @Test
    @DisplayName("탈퇴한 수신자의 테스트 알림을 거부한다")
    void rejectsWithdrawnRecipients() {
        // given
        given(recipients.findByEmail(EMAIL)).willReturn(Optional.of(recipient(true, true)));

        // when & then
        try (var security = mockStatic(SecurityUtil.class)) {
            security.when(SecurityUtil::getCurrentUserEmail).thenReturn(EMAIL);

            assertThatThrownBy(service::sendTestNotification).isInstanceOfSatisfying(CustomException.class,
                    exception -> assertThat(exception.getErrorCode())
                            .isEqualTo(ErrorCode.USER_UNAUTHORIZED));
            verifyNoInteractions(notifications);
        }
    }

    @Test
    @DisplayName("존재하지 않는 수신자의 테스트 알림을 거부한다")
    void rejectsMissingRecipients() {
        // given
        given(recipients.findByEmail(EMAIL)).willReturn(Optional.empty());

        // when & then
        try (var security = mockStatic(SecurityUtil.class)) {
            security.when(SecurityUtil::getCurrentUserEmail).thenReturn(EMAIL);

            assertThatThrownBy(service::sendTestNotification).isInstanceOf(CustomException.class);
            verifyNoInteractions(notifications);
        }
    }

    private NotificationRecipient recipient(boolean deleted, boolean emailEnabled) {
        return new NotificationRecipient(1L, deleted, EMAIL, EMAIL, emailEnabled, false, false, null);
    }
}
