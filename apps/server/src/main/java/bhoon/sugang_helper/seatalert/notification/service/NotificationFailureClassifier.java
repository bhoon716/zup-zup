package bhoon.sugang_helper.seatalert.notification.service;

import bhoon.sugang_helper.common.api.error.CustomException;
import bhoon.sugang_helper.seatalert.notification.exception.NotificationProviderException;

public final class NotificationFailureClassifier {

    private NotificationFailureClassifier() {
        throw new IllegalStateException("유틸리티 클래스는 인스턴스를 생성할 수 없습니다.");
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
