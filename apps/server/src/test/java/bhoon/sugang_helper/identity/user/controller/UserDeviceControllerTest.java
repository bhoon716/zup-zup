package bhoon.sugang_helper.identity.user.controller;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import bhoon.sugang_helper.identity.user.dto.command.RegisterDeviceCommand;
import bhoon.sugang_helper.identity.user.dto.result.UserDeviceResult;
import bhoon.sugang_helper.identity.user.entity.User;
import bhoon.sugang_helper.identity.user.service.UserDeviceService;
import java.net.URI;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
class UserDeviceControllerTest {

    private MockMvc mockMvc;

    @Mock
    private UserDeviceService userDeviceService;

    @InjectMocks
    private UserDeviceController userDeviceController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(userDeviceController).build();
    }

    @Test
    void registrationAcceptsWebPushKeysWithoutType() throws Exception {
        // given
        String endpoint = "https://fcm.googleapis.com/fixture-web-push-endpoint";
        String body = """
                {"token":"https://fcm.googleapis.com/fixture-web-push-endpoint",
                 "p256dh":"fixture-public-key","auth":"fixture-auth-key","alias":"내 브라우저"}
                """;

        // when
        ResultActions response = mockMvc.perform(post("/api/v1/users/devices")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));

        // then
        response.andExpect(status().isOk());
        verify(userDeviceService).registerDevice(new RegisterDeviceCommand(
                endpoint, "fixture-public-key", "fixture-auth-key", "내 브라우저"));
        verifyNoMoreInteractions(userDeviceService);
    }

    @Test
    void deviceListContainsDisplayFieldsWithoutTypeOrPrivateKeys() throws Exception {
        // given
        User user = User.builder().id(7L).email("user@example.invalid").build();
        UserDeviceResult device = new UserDeviceResult(
                11L, "내 브라우저", "https...point", LocalDateTime.of(2026, 11, 5, 12, 0));
        given(userDeviceService.getCurrentUserOrThrow()).willReturn(user);
        given(userDeviceService.getUserDevices(7L)).willReturn(List.of(device));

        // when
        ResultActions response = mockMvc.perform(get("/api/v1/users/devices"));

        // then
        response.andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(11))
                .andExpect(jsonPath("$[0].alias").value("내 브라우저"))
                .andExpect(jsonPath("$[0].maskedToken").value("https...point"))
                .andExpect(jsonPath("$[0].registeredAt").exists())
                .andExpect(jsonPath("$[0].type").doesNotHaveJsonPath())
                .andExpect(jsonPath("$[0].token").doesNotHaveJsonPath())
                .andExpect(jsonPath("$[0].p256dh").doesNotHaveJsonPath())
                .andExpect(jsonPath("$[0].auth").doesNotHaveJsonPath());
        verify(userDeviceService).getCurrentUserOrThrow();
        verify(userDeviceService).getUserDevices(7L);
        verifyNoMoreInteractions(userDeviceService);
    }

    @Test
    void tokenUnregisterAcceptsTheTokenInTheRequestBody() throws Exception {
        // given
        String token = "token/with?unsafe=value";

        // when & then
        mockMvc.perform(delete("/api/v1/users/devices")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"token/with?unsafe=value\"}"))
                .andExpect(status().isOk());

        verify(userDeviceService).unregisterDevice(token);
    }

    @Test
    void legacyTokenPathDecodesEncodedSpecialCharactersForDeployedClients() throws Exception {
        // given
        String token = "token/with?unsafe=value";

        // when & then
        mockMvc.perform(delete(URI.create("/api/v1/users/devices/token/token%2Fwith%3Funsafe%3Dvalue")))
                .andExpect(status().isOk());

        verify(userDeviceService).unregisterDevice(token);
    }
}
