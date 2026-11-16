package bhoon.sugang_helper.seatalert.notification.service;

import bhoon.sugang_helper.common.api.error.CustomException;
import bhoon.sugang_helper.common.api.error.ErrorCode;
import bhoon.sugang_helper.identity.api.notifications.NotificationDevice;
import bhoon.sugang_helper.identity.api.notifications.NotificationRecipient;
import bhoon.sugang_helper.identity.api.notifications.NotificationRecipients;
import bhoon.sugang_helper.identity.api.notifications.WebPushSubscriptionManagement;
import bhoon.sugang_helper.seatalert.api.NotificationChannel;
import bhoon.sugang_helper.seatalert.notification.dto.provider.NotificationTarget;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
class InboxWebPushNotices {

    private final NotificationRecipients recipients;
    private final NotificationChannelPolicy policy;
    private final WebPushSubscriptionManagement subscriptions;
    private final InboxDeliveryNotices notices;

    static boolean isInvalidSubscription(RuntimeException exception) {
        return exception instanceof CustomException customException
                && customException.getErrorCode() == ErrorCode.WEB_PUSH_INVALID_SUBSCRIPTION
                && !NotificationFailureClassifier.isRetryable(exception);
    }

    void recordMissingDevice(NotificationRecipient user, String sourceKey, String courseKey, String courseName) {
        notices.recordReregistration(user.id(), "web:missing:" + sourceKey, courseKey, courseName);
    }

    void recordFailure(NotificationRecipient user, String sourceKey, String courseKey, String courseName,
                       NotificationChannel channel, NotificationTarget target, RuntimeException exception,
                       boolean terminal) {
        if (!policy.isChannelEnabled(user, channel)) {
            return;
        }
        if (channel == NotificationChannel.WEB && isInvalidSubscription(exception)) {
            recordExpiredDevice(user, sourceKey, courseKey, courseName, target.getDevice());
            return;
        }
        if ((terminal || !NotificationFailureClassifier.isRetryable(exception))
                && isCurrentTarget(user, channel, target)) {
            String deviceLabel = channel == NotificationChannel.WEB ? target.getDevice().alias() : null;
            notices.recordFailure(user.id(), sourceKey, courseKey, courseName, channel, deviceLabel);
        }
    }

    boolean hasUsableDevices(NotificationRecipient user) {
        return !policy.resolveDeliveryTargets(user, recipients.findDevicesByUserId(user.id()),
                NotificationChannel.WEB).isEmpty();
    }

    private void recordExpiredDevice(NotificationRecipient user, String sourceKey, String courseKey,
                                     String courseName, NotificationDevice device) {
        if (!subscriptions.deleteInvalidSubscription(device)) {
            return;
        }
        notices.recordFailure(user.id(), sourceKey, courseKey, courseName, NotificationChannel.WEB, device.alias());
        if (!hasUsableDevices(user)) {
            notices.recordReregistration(user.id(), "web:expired:" + device.id(), courseKey, courseName);
        }
    }

    private boolean isCurrentTarget(NotificationRecipient user, NotificationChannel channel, NotificationTarget target) {
        if (channel == NotificationChannel.WEB) {
            return recipients.findDevicesByUserId(user.id()).stream()
                    .anyMatch(target.getDevice()::matchesSubscription);
        }
        return policy.resolveDeliveryTargets(user, List.of(), channel).stream()
                .anyMatch(current -> current.getRecipient().equals(target.getRecipient()));
    }
}
