package bhoon.sugang_helper.identity.user.service;

/**
 * Web Push 전송 경로에서 사용자 기기의 endpoint를 검증하고 무효 endpoint를 정리하는 계약입니다.
 */
public interface UserDeviceWebPushManagement {

    void validateEndpoint(String endpoint);

    void deleteDeviceByToken(String token);
}
