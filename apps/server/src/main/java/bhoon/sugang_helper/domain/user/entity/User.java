package bhoon.sugang_helper.domain.user.entity;

import bhoon.sugang_helper.common.audit.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "users")
public class User extends BaseEntity {

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
    private boolean emailEnabled = true;

    @Column(nullable = false)
    private boolean webPushEnabled = true;

    @Column(nullable = false)
    private boolean fcmEnabled = true;

    @Column(nullable = false)
    private boolean onboardingCompleted = false;

    @Column
    private String discordId;

    @Column(nullable = false)
    private boolean discordEnabled = false;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

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

    public void unlinkDiscord() {
        this.discordId = null;
        this.discordEnabled = false;
    }

    public void completeOnboarding(String notificationEmail, boolean emailEnabled, boolean webPushEnabled) {
        this.notificationEmail = notificationEmail;
        this.emailEnabled = emailEnabled;
        this.webPushEnabled = webPushEnabled;
        this.onboardingCompleted = true;
    }

    public User update(String name) {
        this.name = name;
        return this;
    }

    public String getRoleKey() {
        return this.role.getKey();
    }
}
