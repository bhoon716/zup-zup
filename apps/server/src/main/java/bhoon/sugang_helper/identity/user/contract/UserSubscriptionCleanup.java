package bhoon.sugang_helper.identity.user.contract;

public interface UserSubscriptionCleanup {

    void cancelAllForUser(Long userId);
}
