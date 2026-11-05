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
    void sendsForTheAuthenticatedActiveRecipient() {
        // given
        given(recipients.findByEmail(EMAIL)).willReturn(Optional.of(recipient(false, true)));
        try (var security = mockStatic(SecurityUtil.class)) {
            security.when(SecurityUtil::getCurrentUserEmail).thenReturn(EMAIL);

            // when
            service.sendTestNotification();

            // then
            verify(notifications).sendUserTestNotification(1L);
        }
    }

    @Test
    void rejectsDisabledChannelsBeforeSending() {
        // given
        given(recipients.findByEmail(EMAIL)).willReturn(Optional.of(recipient(false, false)));
        try (var security = mockStatic(SecurityUtil.class)) {
            security.when(SecurityUtil::getCurrentUserEmail).thenReturn(EMAIL);

            // when & then
            assertThatThrownBy(service::sendTestNotification).isInstanceOfSatisfying(CustomException.class,
                    exception -> assertThat(exception.getErrorCode())
                            .isEqualTo(ErrorCode.INVALID_INPUT));
            verifyNoInteractions(notifications);
        }
    }

    @Test
    void rejectsWithdrawnRecipients() {
        // given
        given(recipients.findByEmail(EMAIL)).willReturn(Optional.of(recipient(true, true)));
        try (var security = mockStatic(SecurityUtil.class)) {
            security.when(SecurityUtil::getCurrentUserEmail).thenReturn(EMAIL);

            // when & then
            assertThatThrownBy(service::sendTestNotification).isInstanceOfSatisfying(CustomException.class,
                    exception -> assertThat(exception.getErrorCode())
                            .isEqualTo(ErrorCode.USER_UNAUTHORIZED));
            verifyNoInteractions(notifications);
        }
    }

    @Test
    void rejectsMissingRecipients() {
        // given
        given(recipients.findByEmail(EMAIL)).willReturn(Optional.empty());
        try (var security = mockStatic(SecurityUtil.class)) {
            security.when(SecurityUtil::getCurrentUserEmail).thenReturn(EMAIL);

            // when & then
            assertThatThrownBy(service::sendTestNotification).isInstanceOf(CustomException.class);
            verifyNoInteractions(notifications);
        }
    }

    private NotificationRecipient recipient(boolean deleted, boolean emailEnabled) {
        return new NotificationRecipient(1L, deleted, EMAIL, EMAIL, emailEnabled, false, false, null);
    }
}
