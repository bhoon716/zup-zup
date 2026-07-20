package bhoon.sugang_helper.user.application;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import bhoon.sugang_helper.common.error.CustomException;
import org.junit.jupiter.api.Test;

class WebPushEndpointValidatorTest {

    private final WebPushEndpointValidator validator = new WebPushEndpointValidator();

    @Test
    void acceptsKnownPushServiceEndpoint() {
        assertThatCode(() -> validator.validate("https://fcm.googleapis.com/fcm/send/subscription"))
                .doesNotThrowAnyException();
    }

    @Test
    void rejectsPrivateAndUntrustedEndpoints() {
        assertThatThrownBy(() -> validator.validate("http://127.0.0.1:8080/admin"))
                .isInstanceOf(CustomException.class);
        assertThatThrownBy(() -> validator.validate("https://169.254.169.254/latest/meta-data"))
                .isInstanceOf(CustomException.class);
        assertThatThrownBy(() -> validator.validate("https://evil.example/push"))
                .isInstanceOf(CustomException.class);
    }
}
