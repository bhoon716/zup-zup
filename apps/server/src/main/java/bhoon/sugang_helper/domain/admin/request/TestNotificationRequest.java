package bhoon.sugang_helper.domain.admin.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Schema(description = "관리자 알림 테스트 요청")
public class TestNotificationRequest {

    @Schema(description = "수신자 이메일", example = "test@example.com")
    @Email(message = "올바른 이메일 형식이 아닙니다.")
    @NotEmpty(message = "이메일은 필수 입력값입니다.")
    private String email;

    @Schema(description = "발송 채널 리스트 (EMAIL, WEB, FCM)", example = "[\"EMAIL\", \"WEB\"]")
    @NotEmpty(message = "최소 하나 이상의 채널을 선택해야 합니다.")
    private List<String> channels;
}
