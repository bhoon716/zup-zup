package bhoon.sugang_helper.user.domain;

import bhoon.sugang_helper.common.audit.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import bhoon.sugang_helper.notification.infra.NotificationChannel;
import java.util.ArrayList;
import java.util.List;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "users")
public class User extends BaseEntity {

    private static final String WITHDRAWN_USER_NAME = "탈퇴한 사용자";
    private static final String WITHDRAWN_EMAIL_DOMAIN = "@deleted.invalid";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String email;

    @Column
    private String notificationEmail;

    @Column(nullable = false)
    private boolean emailEnabled;

    @Column(nullable = false)
    private boolean webPushEnabled;

    @Column(nullable = false)
    private boolean fcmEnabled;

    @Column(nullable = false)
    private boolean onboardingCompleted;

    @Column
    private String discordId;

    @Column(nullable = false)
    private boolean discordEnabled;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    @Version
    private Long version;

    @Builder
    public User(Long id, String name, String email, String notificationEmail, boolean emailEnabled,
                boolean webPushEnabled, boolean fcmEnabled, boolean discordEnabled, String discordId,
                boolean onboardingCompleted, Role role) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.notificationEmail = notificationEmail;
        this.emailEnabled = emailEnabled;
        this.webPushEnabled = webPushEnabled;
        this.fcmEnabled = fcmEnabled;
        this.discordEnabled = discordEnabled;
        this.discordId = discordId;
        this.onboardingCompleted = onboardingCompleted;
        this.role = role;
    }

    public void updateSettings(String notificationEmail, boolean emailEnabled, boolean webPushEnabled,
                               boolean fcmEnabled, boolean discordEnabled) {
        this.notificationEmail = notificationEmail;
        this.emailEnabled = emailEnabled;
        this.webPushEnabled = webPushEnabled;
        this.fcmEnabled = fcmEnabled;
        this.discordEnabled = discordEnabled;
    }

    public void linkDiscord(String discordId) {
        this.discordId = discordId;
        this.discordEnabled = true;
    }

    @SuppressWarnings("PMD.NullAssignment")
    public void unlinkDiscord() {
        this.discordId = null;
        this.discordEnabled = false;
    }

    public void completeOnboarding(String notificationEmail, boolean emailEnabled, boolean webPushEnabled,
                                   boolean fcmEnabled, boolean discordEnabled) {
        this.notificationEmail = notificationEmail;
        this.emailEnabled = emailEnabled;
        this.webPushEnabled = webPushEnabled;
        this.fcmEnabled = fcmEnabled;
        this.discordEnabled = discordEnabled;
        this.onboardingCompleted = true;
    }

    public User update(String name) {
        this.name = name;
        return this;
    }

    @SuppressWarnings("PMD.NullAssignment")
    public void withdraw() {
        this.name = WITHDRAWN_USER_NAME;
        this.email = "deleted-" + this.id + WITHDRAWN_EMAIL_DOMAIN;
        this.notificationEmail = null;
        this.emailEnabled = false;
        this.webPushEnabled = false;
        this.fcmEnabled = false;
        this.discordId = null;
        this.discordEnabled = false;
        this.onboardingCompleted = false;
        markDeleted();
    }

    public String getRoleKey() {
        return this.role.getKey();
    }

    public List<NotificationChannel> getEnabledNotificationChannels() {
        List<NotificationChannel> channels = new ArrayList<>();
        if (this.emailEnabled) {
            channels.add(NotificationChannel.EMAIL);
        }
        if (this.fcmEnabled) {
            channels.add(NotificationChannel.FCM);
        }
        if (this.webPushEnabled) {
            channels.add(NotificationChannel.WEB);
        }
        if (this.discordEnabled) {
            channels.add(NotificationChannel.DISCORD);
        }
        return channels;
    }
}
