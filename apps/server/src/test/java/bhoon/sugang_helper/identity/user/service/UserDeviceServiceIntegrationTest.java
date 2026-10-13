package bhoon.sugang_helper.identity.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import bhoon.sugang_helper.identity.user.entity.User;
import bhoon.sugang_helper.identity.user.entity.UserDevice;
import bhoon.sugang_helper.identity.user.enums.DeviceType;
import bhoon.sugang_helper.identity.user.enums.Role;
import bhoon.sugang_helper.identity.user.repository.UserDeviceRepository;
import bhoon.sugang_helper.identity.user.repository.UserRepository;
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
    private UserRepository userRepository;

    @Autowired
    private UserDeviceRepository userDeviceRepository;

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
