package bhoon.sugang_helper.user.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum DeviceType {
    FCM("FCM"),
    WEB("WEB");

    private final String value;
}
