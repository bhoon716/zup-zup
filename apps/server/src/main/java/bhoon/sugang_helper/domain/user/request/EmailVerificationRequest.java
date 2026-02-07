package bhoon.sugang_helper.domain.user.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Schema(description = "이메일 인증 코드 검증 요청")
public class EmailVerificationRequest {

    @Schema(description = "인증할 이메일", example = "test@example.com")
    @NotBlank(message = "이메일은 필수입니다.")
    @Email(message = "올바른 이메일 형식이 아닙니다.")
    private String email;

    @Schema(description = "인증 코드", example = "123456")
    @NotBlank(message = "인증 코드는 필수입니다.")
    private String code;
}
