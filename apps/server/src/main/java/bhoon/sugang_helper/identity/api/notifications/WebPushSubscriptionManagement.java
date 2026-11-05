package bhoon.sugang_helper.identity.api.notifications;

/** 웹 푸시 발송에 필요한 endpoint 검증과 만료 구독 관리를 제공한다. */
public interface WebPushSubscriptionManagement {

    void validateEndpoint(String endpoint);

    void deleteDeviceByToken(String token);
}
