package bhoon.sugang_helper.identity.user.service;

import bhoon.sugang_helper.identity.api.notifications.NotificationDevice;
import bhoon.sugang_helper.identity.api.notifications.NotificationRecipient;
import bhoon.sugang_helper.identity.api.notifications.NotificationRecipients;
import bhoon.sugang_helper.identity.user.entity.User;
import bhoon.sugang_helper.identity.user.entity.UserDevice;
import bhoon.sugang_helper.identity.user.repository.UserDeviceRepository;
import bhoon.sugang_helper.identity.user.repository.UserRepository;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserNotificationQueryService implements NotificationRecipients {

    private final UserRepository userRepository;
    private final UserDeviceRepository userDeviceRepository;

    public Optional<NotificationRecipient> findById(Long userId) {
        return userRepository.findById(userId).map(UserNotificationQueryService::toRecipient);
    }

    public Optional<NotificationRecipient> findByEmail(String email) {
        return userRepository.findByEmail(email).map(UserNotificationQueryService::toRecipient);
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public List<NotificationRecipient> findActiveByIdsForShare(List<Long> userIds) {
        return userRepository.findActiveByIdsForShare(userIds).stream()
                .map(UserNotificationQueryService::toRecipient)
                .toList();
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public Optional<NotificationRecipient> findActiveByIdForUpdate(Long userId) {
        return userRepository.findActiveByIdForUpdate(userId).map(UserNotificationQueryService::toRecipient);
    }

    public Optional<Long> findLastActiveId() {
        return userRepository.findLastActiveId();
    }

    public List<Long> findActiveIdsBetween(Long afterId, Long throughId, int limit) {
        return userRepository.findActiveIdsBetween(afterId, throughId, PageRequest.of(0, limit));
    }

    public List<NotificationDevice> findDevicesByUserId(Long userId) {
        return userDeviceRepository.findByUserId(userId).stream()
                .map(UserNotificationQueryService::toNotificationDevice)
                .toList();
    }

    public List<NotificationDevice> findDevicesByUserIds(List<Long> userIds) {
        return userDeviceRepository.findByUserIdIn(userIds).stream()
                .map(UserNotificationQueryService::toNotificationDevice)
                .toList();
    }

    private static NotificationRecipient toRecipient(User user) {
        return toRecipient(user, user.getNotificationEmail());
    }

    static NotificationRecipient toRecipient(User user, String notificationEmail) {
        return new NotificationRecipient(
                user.getId(),
                user.isDeleted(),
                user.getEmail(),
                notificationEmail,
                user.isEmailEnabled(),
                user.isWebPushEnabled(),
                user.isDiscordEnabled(),
                user.getDiscordId());
    }

    private static NotificationDevice toNotificationDevice(UserDevice device) {
        return new NotificationDevice(device.getId(), device.getUserId(), device.getToken(),
                device.getP256dh(), device.getAuth(), device.getAlias());
    }
}
