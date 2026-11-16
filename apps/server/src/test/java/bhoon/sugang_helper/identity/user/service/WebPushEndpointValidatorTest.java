package bhoon.sugang_helper.identity.user.service;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import bhoon.sugang_helper.common.api.error.CustomException;
import bhoon.sugang_helper.common.api.error.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class WebPushEndpointValidatorTest {

    private final WebPushEndpointValidator validator = new WebPushEndpointValidator();

    @ParameterizedTest
    @DisplayName("알려진 푸시 서비스 endpoint를 허용한다")
    @ValueSource(strings = {
            "https://fcm.googleapis.com/fcm/send/subscription",
            "https://updates.push.services.mozilla.com/wpush/v2/subscription",
            "https://web.push.apple.com/subscription",
            "https://JMT17.GOOGLE.COM:443/fcm/send/client_id:fixture-token_1"
    })
    void acceptsKnownPushServiceEndpoint(String endpoint) {
        // when & then
        assertThatCode(() -> validator.validate(endpoint)).doesNotThrowAnyException();
    }

    @ParameterizedTest
    @DisplayName("Chromium 테스트 푸시 주소는 정확한 호스트와 등록 경로만 허용한다")
    @ValueSource(strings = {
            "https://jmt17.google.com.evil.example/fcm/send/fixture",
            "https://jmt18.google.com/fcm/send/fixture",
            "http://jmt17.google.com/fcm/send/fixture",
            "https://jmt17.google.com:444/fcm/send/fixture",
            "https://user@jmt17.google.com/fcm/send/fixture",
            "https://jmt17.google.com/admin",
            "https://jmt17.google.com/fcm/send/",
            "https://jmt17.google.com/fcm/send/fixture/other",
            "https://jmt17.google.com/fcm/send/fixture/../other",
            "https://jmt17.google.com/fcm/send/%2e%2e",
            "https://jmt17.google.com/fcm/send/fixture%2Fother",
            "https://jmt17.google.com/fcm/send/fixture?foo=bar",
            "https://jmt17.google.com/fcm/send/fixture#anchor"
    })
    void rejectsUnsupportedChromiumStagingPushUrls(String endpoint) {
        // when & then
        assertThatThrownBy(() -> validator.validate(endpoint))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_INPUT);
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
