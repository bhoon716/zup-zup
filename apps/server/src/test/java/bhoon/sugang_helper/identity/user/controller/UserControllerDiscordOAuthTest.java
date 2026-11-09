package bhoon.sugang_helper.identity.user.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import bhoon.sugang_helper.identity.auth.service.AuthService;
import bhoon.sugang_helper.identity.user.service.DiscordOAuthService;
import bhoon.sugang_helper.identity.user.service.UserService;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import jakarta.servlet.http.HttpSession;
import java.lang.reflect.Method;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.slf4j.LoggerFactory;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.bind.annotation.RequestParam;

@SuppressWarnings("PMD.AvoidDuplicateLiterals")
class UserControllerDiscordOAuthTest {

    private UserService userService;
    private DiscordOAuthService discordOAuthService;
    private AuthService authSessionLogout;
    private UserController controller;

    @BeforeEach
    void setUp() {
        userService = Mockito.mock(UserService.class);
        discordOAuthService = Mockito.mock(DiscordOAuthService.class);
        authSessionLogout = Mockito.mock(AuthService.class);
        controller = new UserController(userService, discordOAuthService, authSessionLogout);
        ReflectionTestUtils.setField(controller, "frontendBaseUrl", "https://web.example.com");
        ReflectionTestUtils.setField(controller, "discordClientId", "client-id");
        ReflectionTestUtils.setField(controller, "discordRedirectUri", "https://api.example.com/api/v1/users/discord/callback");
    }

    @Test
    @DisplayName("Discord 인증은 설정된 콜백 주소를 사용한다")
    void authorizationUsesConfiguredDiscordCallback() {
        // given
        String location = controller.startDiscordAuthorization("/settings", new MockHttpSession())
                .getHeaders().getFirst("Location");

        // when & then
        assertThat(location).contains(
                "redirect_uri=https%3A%2F%2Fapi.example.com%2Fapi%2Fv1%2Fusers%2Fdiscord%2Fcallback");
    }

    @Test
    @DisplayName("다른 세션의 OAuth state를 거부한다")
    void rejectsStateFromAnotherSession() {
        // given
        MockHttpSession authorizationSession = new MockHttpSession();
        controller.startDiscordAuthorization("/settings", authorizationSession);

        // when
        var response = controller.discordCallback("attacker-code", "wrong-state", null, new MockHttpSession());

        // then
        assertThat(response.getHeaders().getFirst("Location")).isEqualTo("https://web.example.com/settings?discord=error");
        verify(discordOAuthService, never()).exchangeCodeForToken(Mockito.anyString());
        verify(userService, never()).linkDiscordId(Mockito.anyString());
    }

    @Test
    @DisplayName("콜백 성공 후 state를 소모하고 재사용을 거부한다")
    void consumesStateAfterSuccessfulCallbackAndRejectsReplay() {
        // given
        MockHttpSession session = new MockHttpSession();
        String location = controller.startDiscordAuthorization("/onboarding", session).getHeaders().getFirst("Location");
        String state = location.substring(location.lastIndexOf("state=") + "state=".length());
        Mockito.when(discordOAuthService.exchangeCodeForToken("code")).thenReturn("access-token");
        Mockito.when(discordOAuthService.getDiscordUserId("access-token")).thenReturn("discord-user");

        var first = controller.discordCallback("code", state, null, session);
        var replay = controller.discordCallback("code", state, null, session);

        // when & then
        assertThat(first.getHeaders().getFirst("Location")).isEqualTo("https://web.example.com/onboarding?discord=success");
        assertThat(replay.getHeaders().getFirst("Location")).isEqualTo("https://web.example.com/settings?discord=error");
        verify(userService).linkDiscordId("discord-user");
    }

    @Test
    @DisplayName("거부된 콜백은 OAuth state를 로그에 남기지 않는다")
    void rejectedCallbackDoesNotLogOAuthState() {
        // given
        String state = "oauth-state-should-not-appear";
        Logger logger = (Logger) LoggerFactory.getLogger(UserController.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);

        // when & then
        try {
            controller.discordCallback("oauth-code", state, null, new MockHttpSession());

            String messages = appender.list.stream()
                    .map(ILoggingEvent::getFormattedMessage)
                    .collect(Collectors.joining("\n"));
            assertThat(messages).doesNotContain(state);
        } finally {
            logger.detachAppender(appender);
            appender.stop();
        }
    }

    @Test
    @DisplayName("제공자 오류는 토큰 교환 없이 이동하고 OAuth 세션을 정리한다")
    void providerErrorRedirectsWithoutTokenExchangeAndClearsOAuthSession() {
        // given
        MockHttpSession session = new MockHttpSession();
        String location = controller.startDiscordAuthorization("/onboarding", session)
                .getHeaders().getFirst("Location");
        String state = location.substring(location.lastIndexOf("state=") + "state=".length());

        // when
        var response = controller.discordCallback(null, state, "access_denied", session);

        // then
        assertThat(response.getHeaders().getFirst("Location"))
                .isEqualTo("https://web.example.com/onboarding?discord=error");
        verify(discordOAuthService, never()).exchangeCodeForToken(Mockito.anyString());
        verify(userService, never()).linkDiscordId(Mockito.anyString());
        assertThat(session.getAttribute("DISCORD_OAUTH_STATE")).isNull();
        assertThat(session.getAttribute("DISCORD_OAUTH_RETURN_PATH")).isNull();
    }

    @Test
    @DisplayName("콜백 매개변수가 누락되면 토큰 교환 없이 이동한다")
    void missingCallbackParametersRedirectWithoutTokenExchange() {
        // given
        MockHttpSession missingCodeSession = new MockHttpSession();
        String codeStateLocation = controller.startDiscordAuthorization("/settings", missingCodeSession)
                .getHeaders().getFirst("Location");
        String state = codeStateLocation.substring(codeStateLocation.lastIndexOf("state=") + "state=".length());

        var missingCode = controller.discordCallback(null, state, null, missingCodeSession);

        MockHttpSession missingStateSession = new MockHttpSession();
        controller.startDiscordAuthorization("/settings", missingStateSession);
        var missingState = controller.discordCallback("oauth-code", null, null, missingStateSession);

        // when & then
        assertThat(missingCode.getHeaders().getFirst("Location"))
                .isEqualTo("https://web.example.com/settings?discord=error");
        assertThat(missingState.getHeaders().getFirst("Location"))
                .isEqualTo("https://web.example.com/settings?discord=error");
        verify(discordOAuthService, never()).exchangeCodeForToken(Mockito.anyString());
        verify(userService, never()).linkDiscordId(Mockito.anyString());
    }

    @Test
    @DisplayName("제공자의 실패 콜백에서는 매개변수를 선택적으로 받는다")
    void callbackFailureParametersAreOptionalForProviderRedirects() throws NoSuchMethodException {
        // given
        Method method = UserController.class.getMethod(
                "discordCallback", String.class, String.class, String.class, HttpSession.class);

        // when & then
        assertThat(method.getParameters()[0].getAnnotation(RequestParam.class).required()).isFalse();
        assertThat(method.getParameters()[1].getAnnotation(RequestParam.class).required()).isFalse();
        assertThat(method.getParameters()[2].getAnnotation(RequestParam.class).required()).isFalse();
    }

    @Test
    @DisplayName("탈퇴는 현재 인증 쿠키와 세션을 폐기한다")
    void withdrawInvalidatesCurrentAuthenticationCookiesAndSession() {
        // given
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        // when
        var result = controller.withdraw(request, response);

        // then
        assertThat(result.getStatusCode().value()).isEqualTo(200);
        assertThat(result.getBody()).isNull();
        verify(userService).withdraw();
        verify(authSessionLogout).logout(request, response);
    }
}
