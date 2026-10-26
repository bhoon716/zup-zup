package bhoon.sugang_helper.identity.user.dto.request;

import bhoon.sugang_helper.identity.user.enums.DeviceType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserDeviceRequest {

    @NotNull
    private DeviceType type;

    @NotBlank
    @Size(max = 500)
    private String token;

    @Size(max = 500)
    private String p256dh;

    @Size(max = 500)
    private String auth;

    private String alias;
}
