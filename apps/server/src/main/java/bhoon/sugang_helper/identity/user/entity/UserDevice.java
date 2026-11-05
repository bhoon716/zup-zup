package bhoon.sugang_helper.identity.user.entity;

import bhoon.sugang_helper.common.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "user_devices")
public class UserDevice extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false, unique = true, length = 500)
    private String token;

    @Column(length = 500)
    private String p256dh;

    @Column(length = 500)
    private String auth;

    @Column
    private String alias;

    @Builder
    public UserDevice(Long userId, String token, String p256dh, String auth, String alias) {
        this.userId = userId;
        this.token = token;
        this.p256dh = p256dh;
        this.auth = auth;
        this.alias = alias;
    }

    public void updateToken(Long userId, String token, String p256dh, String auth, String alias) {
        this.userId = userId;
        this.token = token;
        this.p256dh = p256dh;
        this.auth = auth;
        if (alias != null && !alias.isBlank()) {
            this.alias = alias;
        }
    }

    public void updateAlias(String alias) {
        this.alias = alias;
    }
}
