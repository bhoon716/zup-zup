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
        User user = getCurrentUserOrThrow();

        userDeviceRepository.findByToken(request.getToken())
                .ifPresentOrElse(
                        device -> {
                            device.updateToken(request.getToken(), request.getP256dh(), request.getAuth(),
                                    request.getAlias());
                            log.info("[UserDevice] Updated existing device: userId={}, token={}, alias={}",
                                    user.getId(),
                                    request.getToken(), request.getAlias());
                        },
                        () -> {
                            UserDevice newDevice = UserDevice.builder()
                                    .userId(user.getId())
                                    .type(request.getType())
                                    .token(request.getToken())
                                    .p256dh(request.getP256dh())
                                    .auth(request.getAuth())
                                    .alias(request.getAlias())
                                    .build();
                            userDeviceRepository.save(newDevice);
                            log.info("[UserDevice] Registered new device: userId={}, token={}, alias={}", user.getId(),
                                    request.getToken(), request.getAlias());
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

    @Transactional
    public void deleteDeviceById(Long deviceId) {
        User user = getCurrentUserOrThrow();
        UserDevice device = userDeviceRepository.findById(deviceId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND, "기기를 찾을 수 없습니다."));

        if (!device.getUserId().equals(user.getId())) {
            throw new CustomException(ErrorCode.FORBIDDEN);
        }

        userDeviceRepository.delete(device);
        log.info("[UserDevice] Deleted device by ID: userId={}, deviceId={}", user.getId(), deviceId);
    }

    /**
     * WebPush 발송 실패(404/410) 시 호출되는 Self-Healing 메서드
     */
    @Transactional
    public void deleteDeviceByToken(String token) {
        userDeviceRepository.findByToken(token)
                .ifPresent(device -> {
                    userDeviceRepository.delete(device);
                    log.info("[UserDevice] Auto-deleted invalid device: token={}", token);
                });
    }

    public List<bhoon.sugang_helper.domain.user.response.UserDeviceResponse> getUserDevices(Long userId) {
        return userDeviceRepository.findByUserId(userId).stream()
                .map(bhoon.sugang_helper.domain.user.response.UserDeviceResponse::from)
                .toList();
    }

    public User getCurrentUserOrThrow() {
        String email = SecurityUtil.getCurrentUserEmail();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_UNAUTHORIZED));
    }
}
