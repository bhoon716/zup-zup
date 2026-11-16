package bhoon.sugang_helper.seatalert.notification.service;

import java.util.function.Supplier;
import org.springframework.dao.DataAccessException;
import org.springframework.transaction.TransactionException;

final class NotificationDeliveryRetry {

    private static final int MAX_SETTLEMENT_ATTEMPTS = 3;
    private static final int MAX_RETRY_BACKOFF_EXPONENT = 8;

    private NotificationDeliveryRetry() {
    }

    static long backoffSeconds(int attempt) {
        return 1L << Math.min(attempt - 1, MAX_RETRY_BACKOFF_EXPONENT);
    }

    static <T> T settle(Supplier<T> operation) {
        for (int attempt = 1; ; attempt++) {
            try {
                return operation.get();
            } catch (DataAccessException | TransactionException exception) {
                if (attempt >= MAX_SETTLEMENT_ATTEMPTS) {
                    throw exception;
                }
            }
        }
    }
}
