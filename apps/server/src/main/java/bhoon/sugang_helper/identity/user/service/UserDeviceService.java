package bhoon.sugang_helper.identity.user.service;

import bhoon.sugang_helper.common.error.CustomException;
import bhoon.sugang_helper.common.error.ErrorCode;
import bhoon.sugang_helper.common.util.SecurityUtil;
import bhoon.sugang_helper.identity.user.dto.command.RegisterDeviceCommand;
import bhoon.sugang_helper.identity.user.dto.result.UserDeviceResult;
import bhoon.sugang_helper.identity.user.entity.User;
import bhoon.sugang_helper.identity.user.entity.UserDevice;
import bhoon.sugang_helper.identity.user.enums.DeviceType;
import bhoon.sugang_helper.identity.user.repository.UserDeviceRepository;
import bhoon.sugang_helper.identity.user.repository.UserRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserDeviceService implements UserDeviceWebPushManagement {

    private final UserDeviceRepository userDeviceRepository;
    private final UserRepository userRepository;
    private final WebPushEndpointValidator webPushEndpointValidator;

    @Transactional
    public void registerDevice(RegisterDeviceCommand command) {
        User user = getCurrentUserOrThrow();
        if (command.type() == DeviceType.WEB) {
            webPushEndpointValidator.validate(command.token());
        }

        userDeviceRepository.findByToken(command.token())
                .ifPresentOrElse(
                        device -> {
                            device.updateToken(user.getId(), command.token(), command.p256dh(), command.auth(),
                                    command.alias());
                            log.info("[UserDevice] Updated existing device: userId={}, tokenFingerprint={}, alias={}",
                                    user.getId(),
                                    tokenFingerprint(command.token()), command.alias());
                        },
                        () -> {
                            UserDevice newDevice = UserDevice.builder()
                                    .userId(user.getId())
                                    .type(command.type())
                                    .token(command.token())
                                    .p256dh(command.p256dh())
                                    .auth(command.auth())
                                    .alias(command.alias())
                                    .build();
                            userDeviceRepository.save(newDevice);
                            log.info("[UserDevice] Registered new device: userId={}, tokenFingerprint={}, alias={}",
                                    user.getId(), tokenFingerprint(command.token()), command.alias());
                        });
    }

    @Transactional
    public void unregisterDevice(String token) {
        User user = getCurrentUserOrThrow();
        long deletedCount = userDeviceRepository.deleteByTokenAndUserId(token, user.getId());
        if (deletedCount > 0) {
            log.info("[UserDevice] Unregistered device: userId={}, tokenFingerprint={}",
                    user.getId(), tokenFingerprint(token));
        }
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
                    log.info("[UserDevice] Auto-deleted invalid device: tokenFingerprint={}", tokenFingerprint(token));
                });
    }

    @Override
    public void validateEndpoint(String endpoint) {
        webPushEndpointValidator.validate(endpoint);
    }

    public List<UserDeviceResult> getUserDevices(Long userId) {
        return userDeviceRepository.findByUserId(userId).stream()
                .map(UserDeviceResult::from)
                .toList();
    }

    public User getCurrentUserOrThrow() {
        String email = SecurityUtil.getCurrentUserEmail();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_UNAUTHORIZED));
    }

    private String tokenFingerprint(String token) {
        if (token == null) {
            return "<none>";
        }

        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8));
            return "sha256:" + HexFormat.of().formatHex(digest, 0, 6);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is required to fingerprint device tokens", e);
        }
    }
}
