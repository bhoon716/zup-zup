package bhoon.sugang_helper.user.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import bhoon.sugang_helper.common.error.CustomException;
import bhoon.sugang_helper.common.error.ErrorCode;
import org.junit.jupiter.api.Test;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

class DiscordOAuthServiceTest {

    @Test
    void discordRestClientUsesBoundedConnectAndReadTimeouts() {
        SimpleClientHttpRequestFactory requestFactory = DiscordOAuthService.createRequestFactory();

        assertThat(ReflectionTestUtils.getField(requestFactory, "connectTimeout")).isEqualTo(3_000);
        assertThat(ReflectionTestUtils.getField(requestFactory, "readTimeout")).isEqualTo(5_000);
    }

    @Test
    void tokenExchangeTimeoutIsConvertedToCustomException() {
        RestClient restClient = mock(RestClient.class, RETURNS_DEEP_STUBS);
        when(restClient.post()
                .uri("/oauth2/token")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(any(MultiValueMap.class))
                .retrieve()
                .body(any(ParameterizedTypeReference.class)))
                .thenThrow(new ResourceAccessException("Read timed out"));
        DiscordOAuthService service = new DiscordOAuthService(restClient);
        ReflectionTestUtils.setField(service, "clientId", "client-id");
        ReflectionTestUtils.setField(service, "clientSecret", "client-secret");

        assertThatThrownBy(() -> service.exchangeCodeForToken("code"))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_INPUT);
    }

    @Test
    void userLookupTimeoutIsConvertedToCustomException() {
        RestClient restClient = mock(RestClient.class);
        RestClient.RequestHeadersUriSpec<?> getSpec = mock(RestClient.RequestHeadersUriSpec.class);
        RestClient.RequestHeadersSpec<?> requestSpec = mock(RestClient.RequestHeadersSpec.class);
        RestClient.ResponseSpec responseSpec = mock(RestClient.ResponseSpec.class);
        doReturn(getSpec).when(restClient).get();
        doReturn(requestSpec).when(getSpec).uri("/users/@me");
        doReturn(requestSpec).when(requestSpec).header(eq("Authorization"), any(String[].class));
        doReturn(responseSpec).when(requestSpec).retrieve();
        doThrow(new ResourceAccessException("Connect timed out"))
                .when(responseSpec).body(any(ParameterizedTypeReference.class));
        DiscordOAuthService service = new DiscordOAuthService(restClient);

        assertThatThrownBy(() -> service.getDiscordUserId("access-token"))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_INPUT);
    }
}
