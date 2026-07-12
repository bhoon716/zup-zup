package bhoon.sugang_helper.user.presentation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import bhoon.sugang_helper.user.application.DiscordOAuthService;
import bhoon.sugang_helper.user.application.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.util.ReflectionTestUtils;

@SuppressWarnings("PMD.AvoidDuplicateLiterals") // Redirect assertions intentionally repeat the HTTP header name.
class UserControllerDiscordOAuthTest {

    private UserService userService;
    private DiscordOAuthService discordOAuthService;
    private UserController controller;

    @BeforeEach
    void setUp() {
        userService = Mockito.mock(UserService.class);
        discordOAuthService = Mockito.mock(DiscordOAuthService.class);
        controller = new UserController(userService, discordOAuthService);
        ReflectionTestUtils.setField(controller, "frontendBaseUrl", "https://web.example.com");
        ReflectionTestUtils.setField(controller, "discordClientId", "client-id");
        ReflectionTestUtils.setField(controller, "discordRedirectUri", "https://api.example.com/api/v1/users/discord/callback");
    }

    @Test
    void rejectsStateFromAnotherSession() {
        MockHttpSession authorizationSession = new MockHttpSession();
        controller.startDiscordAuthorization("/settings", authorizationSession);

        var response = controller.discordCallback("attacker-code", "wrong-state", new MockHttpSession());

        assertThat(response.getHeaders().getFirst("Location")).isEqualTo("https://web.example.com/settings?discord=error");
        verify(discordOAuthService, never()).exchangeCodeForToken(Mockito.anyString());
        verify(userService, never()).linkDiscordId(Mockito.anyString());
    }

    @Test
    void consumesStateAfterSuccessfulCallbackAndRejectsReplay() {
        MockHttpSession session = new MockHttpSession();
        String location = controller.startDiscordAuthorization("/onboarding", session).getHeaders().getFirst("Location");
        String state = location.substring(location.lastIndexOf("state=") + "state=".length());
        Mockito.when(discordOAuthService.exchangeCodeForToken("code")).thenReturn("access-token");
        Mockito.when(discordOAuthService.getDiscordUserId("access-token")).thenReturn("discord-user");

        var first = controller.discordCallback("code", state, session);
        var replay = controller.discordCallback("code", state, session);

        assertThat(first.getHeaders().getFirst("Location")).isEqualTo("https://web.example.com/onboarding?discord=success");
        assertThat(replay.getHeaders().getFirst("Location")).isEqualTo("https://web.example.com/settings?discord=error");
        verify(userService).linkDiscordId("discord-user");
    }
}
