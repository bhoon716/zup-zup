package bhoon.sugang_helper.user.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import bhoon.sugang_helper.user.domain.DeviceType;
import bhoon.sugang_helper.user.domain.Role;
import bhoon.sugang_helper.user.domain.User;
import bhoon.sugang_helper.user.domain.UserDevice;
import bhoon.sugang_helper.user.infra.UserDeviceJpaRepository;
import bhoon.sugang_helper.user.infra.UserJpaRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@DataJpaTest
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@Import(UserDeviceService.class)
class UserDeviceServiceIntegrationTest {

    @Autowired
    private UserDeviceService userDeviceService;

    @Autowired
    private UserJpaRepository userRepository;

    @Autowired
    private UserDeviceJpaRepository userDeviceRepository;

    @MockitoBean
    private WebPushEndpointValidator webPushEndpointValidator;

    @AfterEach
    void cleanUp() {
        SecurityContextHolder.clearContext();
        userDeviceRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void tokenUnregisterDoesNotDeleteAnotherUsersDevice() {
        User requester = saveUser("requester@example.com");
        User owner = saveUser("owner@example.com");
        UserDevice device = userDeviceRepository.saveAndFlush(UserDevice.builder()
                .userId(owner.getId())
                .type(DeviceType.WEB)
                .token("owner-device-token")
                .alias("Owner browser")
                .build());
        authenticateAs(requester.getEmail());

        assertThatCode(() -> userDeviceService.unregisterDevice(device.getToken()))
                .doesNotThrowAnyException();

        assertThat(userDeviceRepository.findById(device.getId())).isPresent();
    }

    @Test
    void tokenUnregisterDeletesTheAuthenticatedUsersDevice() {
        User owner = saveUser("owner@example.com");
        UserDevice device = userDeviceRepository.saveAndFlush(UserDevice.builder()
                .userId(owner.getId())
                .type(DeviceType.WEB)
                .token("owner-device-token")
                .alias("Owner browser")
                .build());
        authenticateAs(owner.getEmail());

        userDeviceService.unregisterDevice(device.getToken());

        assertThat(userDeviceRepository.findById(device.getId())).isEmpty();
    }

    private User saveUser(String email) {
        return userRepository.saveAndFlush(User.builder()
                .name(email)
                .email(email)
                .role(Role.USER)
                .build());
    }

    private void authenticateAs(String email) {
        SecurityContextHolder.getContext().setAuthentication(new TestingAuthenticationToken(email, null));
    }
}
