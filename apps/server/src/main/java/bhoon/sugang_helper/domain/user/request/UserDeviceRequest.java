package bhoon.sugang_helper.domain.user.request;

import bhoon.sugang_helper.domain.user.enums.DeviceType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class UserDeviceRequest {

    @NotNull
    private DeviceType type;

    @NotBlank
    private String token;

    // Optional for Web Push
    private String p256dh;
    private String auth;
}
