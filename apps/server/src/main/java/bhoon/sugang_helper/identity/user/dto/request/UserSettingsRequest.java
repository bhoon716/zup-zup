package bhoon.sugang_helper.identity.user.dto.request;

import jakarta.validation.constraints.Email;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserSettingsRequest {

    @Email
    private String notificationEmail;

    private boolean emailEnabled;

    private boolean webPushEnabled;

    private boolean fcmEnabled;

    private boolean discordEnabled;

    public UserSettingsRequest(String notificationEmail, boolean emailEnabled, boolean webPushEnabled,
                               boolean fcmEnabled, boolean discordEnabled) {
        this.notificationEmail = notificationEmail;
        this.emailEnabled = emailEnabled;
        this.webPushEnabled = webPushEnabled;
        this.fcmEnabled = fcmEnabled;
        this.discordEnabled = discordEnabled;
    }
}
