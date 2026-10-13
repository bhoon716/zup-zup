package bhoon.sugang_helper.identity.user.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class UserUpdateRequest {
    @NotBlank(message = "이름은 필수 입력 항목입니다.")
    private String name;
}
