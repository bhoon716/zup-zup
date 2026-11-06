package bhoon.sugang_helper.identity.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import bhoon.sugang_helper.common.api.error.CustomException;
import bhoon.sugang_helper.common.api.error.ErrorCode;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

class DiscordOAuthServiceTest {

    @Test
    @DisplayName("Discord 클라이언트는 연결과 읽기 제한 시간을 사용한다")
    void discordRestClientUsesBoundedConnectAndReadTimeouts() {
        // when
        SimpleClientHttpRequestFactory requestFactory = DiscordOAuthService.createRequestFactory();

        // then
        assertThat(ReflectionTestUtils.getField(requestFactory, "connectTimeout")).isEqualTo(3_000);
        assertThat(ReflectionTestUtils.getField(requestFactory, "readTimeout")).isEqualTo(5_000);
    }

    @Test
    @DisplayName("토큰 교환 제한 시간 초과를 업무 예외로 변환한다")
    void tokenExchangeTimeoutIsConvertedToCustomException() {
        // given
        RestClient restClient = mock(RestClient.class, RETURNS_DEEP_STUBS);
        when(restClient.post()
                .uri("/oauth2/token")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(any(MultiValueMap.class))
                .retrieve()
                .body(ArgumentMatchers.<ParameterizedTypeReference<Map<String, Object>>>any()))
                .thenThrow(new ResourceAccessException("Read timed out"));
        DiscordOAuthService service = new DiscordOAuthService(restClient);
        ReflectionTestUtils.setField(service, "clientId", "client-id");
        ReflectionTestUtils.setField(service, "clientSecret", "client-secret");

        // when & then
        assertThatThrownBy(() -> service.exchangeCodeForToken("code"))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_INPUT);
    }

    @Test
    @DisplayName("사용자 조회 제한 시간 초과를 업무 예외로 변환한다")
    void userLookupTimeoutIsConvertedToCustomException() {
        // given
        RestClient restClient = mock(RestClient.class);
        RestClient.RequestHeadersUriSpec<?> getSpec = mock(RestClient.RequestHeadersUriSpec.class);
        RestClient.RequestHeadersSpec<?> requestSpec = mock(RestClient.RequestHeadersSpec.class);
        RestClient.ResponseSpec responseSpec = mock(RestClient.ResponseSpec.class);
        doReturn(getSpec).when(restClient).get();
        doReturn(requestSpec).when(getSpec).uri("/users/@me");
        doReturn(requestSpec).when(requestSpec).header(eq("Authorization"), any(String[].class));
        doReturn(responseSpec).when(requestSpec).retrieve();
        doThrow(new ResourceAccessException("Connect timed out"))
                .when(responseSpec)
                .body(ArgumentMatchers.<ParameterizedTypeReference<Map<String, Object>>>any());
        DiscordOAuthService service = new DiscordOAuthService(restClient);

        // when & then
        assertThatThrownBy(() -> service.getDiscordUserId("access-token"))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_INPUT);
    }
}
