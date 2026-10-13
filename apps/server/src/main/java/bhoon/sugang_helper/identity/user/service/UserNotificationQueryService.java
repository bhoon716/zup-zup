package bhoon.sugang_helper.identity.user.service;

import bhoon.sugang_helper.identity.user.dto.NotificationDevice;
import bhoon.sugang_helper.identity.user.dto.NotificationRecipient;
import bhoon.sugang_helper.identity.user.entity.User;
import bhoon.sugang_helper.identity.user.entity.UserDevice;
import bhoon.sugang_helper.identity.user.enums.DeviceType;
import bhoon.sugang_helper.identity.user.repository.UserDeviceRepository;
import bhoon.sugang_helper.identity.user.repository.UserRepository;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserNotificationQueryService implements UserNotificationQuery {

    private final UserRepository userRepository;
    private final UserDeviceRepository userDeviceRepository;

    @Override
    public Optional<NotificationRecipient> findById(Long userId) {
        return userRepository.findById(userId).map(UserNotificationQueryService::toRecipient);
    }

    @Override
    public Optional<NotificationRecipient> findByEmail(String email) {
        return userRepository.findByEmail(email).map(UserNotificationQueryService::toRecipient);
    }

    @Override
    public List<NotificationRecipient> findByIds(List<Long> userIds) {
        return userRepository.findAllById(userIds).stream()
                .map(UserNotificationQueryService::toRecipient)
                .toList();
    }

    @Override
    public List<NotificationDevice> findDevicesByUserId(Long userId) {
        return userDeviceRepository.findByUserId(userId).stream()
                .map(UserNotificationQueryService::toNotificationDevice)
                .toList();
    }

    @Override
    public List<NotificationDevice> findDevicesByUserIds(List<Long> userIds) {
        return userDeviceRepository.findByUserIdIn(userIds).stream()
                .map(UserNotificationQueryService::toNotificationDevice)
                .toList();
    }

    private static NotificationRecipient toRecipient(User user) {
        return new NotificationRecipient(
                user.getId(),
                user.isDeleted(),
                user.getEmail(),
                user.getNotificationEmail(),
                user.isEmailEnabled(),
                user.isWebPushEnabled(),
                user.isFcmEnabled(),
                user.isDiscordEnabled(),
                user.getDiscordId());
    }

    private static NotificationDevice toNotificationDevice(UserDevice device) {
        return new NotificationDevice(
                device.getUserId(),
                device.getType() == DeviceType.WEB
                        ? NotificationDevice.Type.WEB : NotificationDevice.Type.FCM,
                device.getToken(),
                device.getP256dh(),
                device.getAuth());
    }
}
