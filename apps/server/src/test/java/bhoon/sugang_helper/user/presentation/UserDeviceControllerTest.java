package bhoon.sugang_helper.user.presentation;

import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import bhoon.sugang_helper.user.application.UserDeviceService;
import java.net.URI;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
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
    void tokenUnregisterAcceptsTheTokenInTheRequestBody() throws Exception {
        String token = "token/with?unsafe=value";

        mockMvc.perform(delete("/api/v1/users/devices")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"token/with?unsafe=value\"}"))
                .andExpect(status().isOk());

        verify(userDeviceService).unregisterDevice(token);
    }

    @Test
    void legacyTokenPathDecodesEncodedSpecialCharactersForDeployedClients() throws Exception {
        String token = "token/with?unsafe=value";

        mockMvc.perform(delete(URI.create("/api/v1/users/devices/token/token%2Fwith%3Funsafe%3Dvalue")))
                .andExpect(status().isOk());

        verify(userDeviceService).unregisterDevice(token);
    }
}
