package bhoon.sugang_helper.identity.api.notifications;

public interface WebPushSubscriptionManagement {

    void validateEndpoint(String endpoint);

    void deleteDeviceByToken(String token);
}
