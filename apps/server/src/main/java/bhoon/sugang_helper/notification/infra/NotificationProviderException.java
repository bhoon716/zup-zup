package bhoon.sugang_helper.notification.infra;

import bhoon.sugang_helper.common.error.CustomException;
import bhoon.sugang_helper.common.error.ErrorCode;
import lombok.Getter;

/**
 * Provider failure with an explicit retry classification for the durable outbox.
 */
@Getter
public class NotificationProviderException extends CustomException {

    private final boolean retryable;
    private final String reason;
    private final Integer statusCode;

    public NotificationProviderException(ErrorCode errorCode, boolean retryable, String reason,
                                        Integer statusCode, Throwable cause) {
        super(errorCode);
        this.retryable = retryable;
        this.reason = reason;
        this.statusCode = statusCode;
        if (cause != null) {
            initCause(cause);
        }
    }
}
