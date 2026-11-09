package bhoon.sugang_helper.identity.user.service;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import bhoon.sugang_helper.common.api.error.CustomException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class WebPushEndpointValidatorTest {

    private final WebPushEndpointValidator validator = new WebPushEndpointValidator();

    @Test
    @DisplayName("알려진 푸시 서비스 endpoint를 허용한다")
    void acceptsKnownPushServiceEndpoint() {
        // when & then
        assertThatCode(() -> validator.validate("https://fcm.googleapis.com/fcm/send/subscription"))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("사설망과 신뢰하지 않는 endpoint를 거부한다")
    void rejectsPrivateAndUntrustedEndpoints() {
        // when & then
        assertThatThrownBy(() -> validator.validate("http://127.0.0.1:8080/admin"))
                .isInstanceOf(CustomException.class);
        assertThatThrownBy(() -> validator.validate("https://169.254.169.254/latest/meta-data"))
                .isInstanceOf(CustomException.class);
        assertThatThrownBy(() -> validator.validate("https://evil.example/push"))
                .isInstanceOf(CustomException.class);
    }
}
