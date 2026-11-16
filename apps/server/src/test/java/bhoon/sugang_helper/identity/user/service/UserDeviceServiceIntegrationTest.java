package bhoon.sugang_helper.identity.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import bhoon.sugang_helper.identity.api.notifications.NotificationDevice;
import bhoon.sugang_helper.identity.user.dto.command.RegisterDeviceCommand;
import bhoon.sugang_helper.identity.user.entity.User;
import bhoon.sugang_helper.identity.user.entity.UserDevice;
import bhoon.sugang_helper.identity.user.enums.Role;
import bhoon.sugang_helper.identity.user.repository.UserDeviceRepository;
import bhoon.sugang_helper.identity.user.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@DataJpaTest
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@Import({UserDeviceService.class, WebPushEndpointValidator.class})
class UserDeviceServiceIntegrationTest {

    private static final String OWNER_EMAIL = "owner@example.com";

    @Autowired
    private UserDeviceService userDeviceService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserDeviceRepository userDeviceRepository;

    @AfterEach
    void cleanUp() {
        SecurityContextHolder.clearContext();
        userDeviceRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    @DisplayName("Chromium 테스트 푸시 주소를 등록하면 기기가 저장된다")
    void registersChromiumStagingPushSubscription() {
        // given
        User owner = saveUser(OWNER_EMAIL);
        authenticateAs(owner.getEmail());
        String endpoint = "https://jmt17.google.com/fcm/send/client_id:fixture-token_1";
        RegisterDeviceCommand command = new RegisterDeviceCommand(endpoint, "public-key", "auth-key", "브라우저");

        // when
        userDeviceService.registerDevice(command);

        // then
        assertThat(userDeviceRepository.findByToken(endpoint)).get()
                .satisfies(device -> {
                    assertThat(device.getUserId()).isEqualTo(owner.getId());
                    assertThat(device.getP256dh()).isEqualTo(command.p256dh());
                    assertThat(device.getAuth()).isEqualTo(command.auth());
                });
    }

    @Test
    @DisplayName("토큰 해제는 다른 사용자의 기기를 삭제하지 않는다")
    void tokenUnregisterDoesNotDeleteAnotherUsersDevice() {
        // given
        User requester = saveUser("requester@example.com");
        User owner = saveUser(OWNER_EMAIL);
        UserDevice device = userDeviceRepository.saveAndFlush(UserDevice.builder()
                .userId(owner.getId())
                .token("owner-device-token")
                .alias("Owner browser")
                .build());
        authenticateAs(requester.getEmail());

        // when & then
        assertThatCode(() -> userDeviceService.unregisterDevice(device.getToken()))
                .doesNotThrowAnyException();

        assertThat(userDeviceRepository.findById(device.getId())).isPresent();
    }

    @Test
    @DisplayName("토큰 해제는 인증된 사용자의 기기를 삭제한다")
    void tokenUnregisterDeletesTheAuthenticatedUsersDevice() {
        // given
        User owner = saveUser(OWNER_EMAIL);
        UserDevice device = userDeviceRepository.saveAndFlush(UserDevice.builder()
                .userId(owner.getId())
                .token("owner-device-token")
                .alias("Owner browser")
                .build());
        authenticateAs(owner.getEmail());

        userDeviceService.unregisterDevice(device.getToken());

        // when & then
        assertThat(userDeviceRepository.findById(device.getId())).isEmpty();
    }

    @Test
    @DisplayName("웹 푸시 만료 결과는 갱신된 구독을 삭제하지 않는다")
    void expiredResultDoesNotDeleteARefreshedSubscription() {
        // given
        User owner = saveUser(OWNER_EMAIL);
        UserDevice device = userDeviceRepository.saveAndFlush(UserDevice.builder()
                .userId(owner.getId())
                .token("owner-device-token")
                .p256dh("old-key")
                .auth("old-auth")
                .alias("브라우저")
                .build());
        NotificationDevice original = notificationDevice(device);
        device.updateToken(owner.getId(), device.getToken(), "new-key", "new-auth", device.getAlias());
        UserDevice refreshed = userDeviceRepository.saveAndFlush(device);

        // when
        boolean originalDeleted = userDeviceService.deleteInvalidSubscription(original);

        // then
        assertThat(originalDeleted).isFalse();
        assertThat(userDeviceRepository.findById(device.getId())).get()
                .extracting(UserDevice::getP256dh).isEqualTo("new-key");

        // when
        boolean refreshedDeleted = userDeviceService.deleteInvalidSubscription(notificationDevice(refreshed));

        // then
        assertThat(refreshedDeleted).isTrue();
        assertThat(userDeviceRepository.findById(device.getId())).isEmpty();
    }

    private static NotificationDevice notificationDevice(UserDevice device) {
        return new NotificationDevice(device.getId(), device.getUserId(), device.getToken(),
                device.getP256dh(), device.getAuth(), device.getAlias());
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
