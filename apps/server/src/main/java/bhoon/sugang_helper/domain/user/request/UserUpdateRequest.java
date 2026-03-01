package bhoon.sugang_helper.domain.user.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@Schema(description = "사용자 정보 수정 요청 DTO")
public class UserUpdateRequest {
    @NotBlank(message = "이름은 필수 입력 항목입니다.")
    @Schema(description = "변경할 이름", example = "홍길순")
    private String name;
}
