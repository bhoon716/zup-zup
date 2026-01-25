package bhoon.sugang_helper.domain.user.service;

import bhoon.sugang_helper.common.error.CustomException;
import bhoon.sugang_helper.common.error.ErrorCode;
import bhoon.sugang_helper.domain.user.entity.User;
import bhoon.sugang_helper.domain.user.entity.UserDevice;
import bhoon.sugang_helper.domain.user.repository.UserDeviceRepository;
import bhoon.sugang_helper.domain.user.repository.UserRepository;
import bhoon.sugang_helper.domain.user.request.UserDeviceRequest;
import bhoon.sugang_helper.common.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserDeviceService {

    private final UserDeviceRepository userDeviceRepository;
    private final UserRepository userRepository;

    @Transactional
    public void registerDevice(UserDeviceRequest request) {
        User user = getCurrentUser();

        userDeviceRepository.findByToken(request.getToken())
                .ifPresentOrElse(
                        device -> {
                            device.updateToken(request.getToken(), request.getP256dh(), request.getAuth());
                            log.info("[UserDevice] Updated existing device: userId={}, token={}", user.getId(),
                                    request.getToken());
                        },
                        () -> {
                            UserDevice newDevice = UserDevice.builder()
                                    .userId(user.getId())
                                    .type(request.getType())
                                    .token(request.getToken())
                                    .p256dh(request.getP256dh())
                                    .auth(request.getAuth())
                                    .build();
                            userDeviceRepository.save(newDevice);
                            log.info("[UserDevice] Registered new device: userId={}, token={}", user.getId(),
                                    request.getToken());
                        });
    }

    @Transactional
    public void unregisterDevice(String token) {
        userDeviceRepository.findByToken(token)
                .ifPresent(device -> {
                    userDeviceRepository.delete(device);
                    log.info("[UserDevice] Unregistered device: token={}", token);
                });
    }

    public List<UserDevice> getUserDevices(Long userId) {
        return userDeviceRepository.findByUserId(userId);
    }

    private User getCurrentUser() {
        String email = SecurityUtil.getCurrentUserEmail();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_UNAUTHORIZED));
    }
}
