package bhoon.sugang_helper.identity.user.service;

public interface UserSubscriptionCleanup {

    void cancelAllForUser(Long userId);
}
