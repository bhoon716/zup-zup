package bhoon.sugang_helper.identity.user.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UserDeviceUnregisterRequest(
        @NotBlank
        @Size(max = 500)
        String token) {
}
