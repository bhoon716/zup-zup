package bhoon.sugang_helper.domain.user.entity;

import bhoon.sugang_helper.common.audit.BaseTimeEntity;
import bhoon.sugang_helper.domain.user.enums.DeviceType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "user_devices", indexes = {
        @Index(name = "idx_user_device_user_id", columnList = "userId"),
        @Index(name = "idx_user_device_token", columnList = "token")
})
public class UserDevice extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DeviceType type;

    @Column(nullable = false, unique = true)
    private String token; // FCM Token or Subscription Endpoint for Web

    // For Web Push
    @Column
    private String p256dh;

    @Column
    private String auth;

    @Builder
    public UserDevice(Long userId, DeviceType type, String token, String p256dh, String auth) {
        this.userId = userId;
        this.type = type;
        this.token = token;
        this.p256dh = p256dh;
        this.auth = auth;
    }

    public void updateToken(String token, String p256dh, String auth) {
        this.token = token;
        this.p256dh = p256dh;
        this.auth = auth;
    }
}
