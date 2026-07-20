package bhoon.sugang_helper.notification.infra;

import bhoon.sugang_helper.common.error.CustomException;

/**
 * Keeps permanent recipient/configuration failures out of the retry budget.
 */
public final class NotificationFailureClassifier {

    private NotificationFailureClassifier() {
        throw new IllegalStateException("Utility class");
    }

    public static boolean isRetryable(Throwable throwable) {
        if (throwable instanceof NotificationProviderException providerException) {
            return providerException.isRetryable();
        }
        if (throwable instanceof CustomException customException) {
            return switch (customException.getErrorCode()) {
                case WEB_PUSH_INVALID_SUBSCRIPTION, WEB_PUSH_MISSING_KEYS -> false;
                default -> true;
            };
        }
        return true;
    }
}
