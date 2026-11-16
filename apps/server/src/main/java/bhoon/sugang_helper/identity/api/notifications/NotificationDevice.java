package bhoon.sugang_helper.identity.api.notifications;

import java.util.Objects;

public record NotificationDevice(
        Long id,
        Long userId,
        String token,
        String p256dh,
        String auth,
        String alias) {

    public boolean matchesSubscription(NotificationDevice other) {
        return id.equals(other.id)
                && userId.equals(other.userId)
                && Objects.equals(token, other.token)
                && Objects.equals(p256dh, other.p256dh)
                && Objects.equals(auth, other.auth);
    }
}
