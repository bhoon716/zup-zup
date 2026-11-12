package bhoon.sugang_helper.identity.user.service;

import bhoon.sugang_helper.common.api.error.CustomException;
import bhoon.sugang_helper.identity.api.notifications.NotificationDevice;
import bhoon.sugang_helper.identity.api.notifications.WebPushSubscriptionManagement;
import bhoon.sugang_helper.common.api.error.ErrorCode;
import bhoon.sugang_helper.common.api.security.SecurityUtil;
import bhoon.sugang_helper.identity.user.dto.command.RegisterDeviceCommand;
import bhoon.sugang_helper.identity.user.dto.result.UserDeviceResult;
import bhoon.sugang_helper.identity.user.entity.User;
import bhoon.sugang_helper.identity.user.entity.UserDevice;
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
public class UserDeviceService implements WebPushSubscriptionManagement {

    private static final String EVENT_KEY = "event";
    private static final String USER_ID_KEY = "userId";
    private static final String TOKEN_FINGERPRINT_KEY = "tokenFingerprint";

    private final UserDeviceRepository userDeviceRepository;
    private final UserRepository userRepository;
    private final WebPushEndpointValidator webPushEndpointValidator;

    @Transactional
    public void registerDevice(RegisterDeviceCommand command) {
        User user = getCurrentUserOrThrow();
        webPushEndpointValidator.validate(command.token());

        userDeviceRepository.findByToken(command.token())
                .ifPresentOrElse(
                        device -> updateDevice(device, user.getId(), command),
                        () -> createDevice(user.getId(), command));
    }

    @Transactional
    public void unregisterDevice(String token) {
        User user = getCurrentUserOrThrow();
        long deletedCount = userDeviceRepository.deleteByTokenAndUserId(token, user.getId());
        if (deletedCount > 0) {
            logNotificationDeviceUnregistered(user.getId(), tokenFingerprint(token));
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
        logNotificationDeviceDeleted(user.getId(), deviceId);
    }

    @Override
    @Transactional
    public void deleteDeviceByToken(String token) {
        userDeviceRepository.findByToken(token)
                .ifPresent(device -> deleteInvalidDevice(device, token));
    }

    @Override
    @Transactional
    public boolean deleteInvalidSubscription(NotificationDevice device) {
        if (userDeviceRepository.deleteMatchingSubscription(device) == 0) {
            return false;
        }
        logNotificationInvalidDeviceDeleted(tokenFingerprint(device.token()));
        return true;
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

    private void updateDevice(UserDevice device, Long userId, RegisterDeviceCommand command) {
        device.updateToken(userId, command.token(), command.p256dh(), command.auth(), command.alias());
        logNotificationDeviceUpdated(userId, tokenFingerprint(command.token()));
    }

    private void createDevice(Long userId, RegisterDeviceCommand command) {
        UserDevice device = UserDevice.builder()
                .userId(userId)
                .token(command.token())
                .p256dh(command.p256dh())
                .auth(command.auth())
                .alias(command.alias())
                .build();
        userDeviceRepository.save(device);
        logNotificationDeviceRegistered(userId, tokenFingerprint(command.token()));
    }

    private void deleteInvalidDevice(UserDevice device, String token) {
        userDeviceRepository.delete(device);
        logNotificationInvalidDeviceDeleted(tokenFingerprint(token));
    }

    private String tokenFingerprint(String token) {
        if (token == null) {
            return "<none>";
        }

        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8));
            return "sha256:" + HexFormat.of().formatHex(digest, 0, 6);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("기기 토큰의 지문을 생성하려면 SHA-256 알고리즘이 필요합니다.", e);
        }
    }

    private void logNotificationDeviceUpdated(Long userId, String tokenFingerprint) {
        log.atInfo().addKeyValue(EVENT_KEY, "notification_device_updated")
                .addKeyValue(USER_ID_KEY, userId)
                .addKeyValue(TOKEN_FINGERPRINT_KEY, tokenFingerprint)
                .log("[UserDevice] 기존 알림 기기 갱신.");
    }

    private void logNotificationDeviceRegistered(Long userId, String tokenFingerprint) {
        log.atInfo().addKeyValue(EVENT_KEY, "notification_device_registered")
                .addKeyValue(USER_ID_KEY, userId)
                .addKeyValue(TOKEN_FINGERPRINT_KEY, tokenFingerprint)
                .log("[UserDevice] 알림 기기 등록.");
    }

    private void logNotificationDeviceUnregistered(Long userId, String tokenFingerprint) {
        log.atInfo().addKeyValue(EVENT_KEY, "notification_device_unregistered")
                .addKeyValue(USER_ID_KEY, userId)
                .addKeyValue(TOKEN_FINGERPRINT_KEY, tokenFingerprint)
                .log("[UserDevice] 알림 기기 등록 해제.");
    }

    private void logNotificationDeviceDeleted(Long userId, Long deviceId) {
        log.atInfo().addKeyValue(EVENT_KEY, "notification_device_deleted")
                .addKeyValue(USER_ID_KEY, userId)
                .addKeyValue("deviceId", deviceId)
                .log("[UserDevice] 지정 알림 기기 삭제.");
    }

    private void logNotificationInvalidDeviceDeleted(String tokenFingerprint) {
        log.atInfo().addKeyValue(EVENT_KEY, "notification_invalid_device_deleted")
                .addKeyValue(TOKEN_FINGERPRINT_KEY, tokenFingerprint)
                .log("[UserDevice] 유효하지 않은 알림 기기 자동 삭제.");
    }

}
