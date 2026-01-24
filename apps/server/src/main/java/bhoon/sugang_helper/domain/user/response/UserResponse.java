package bhoon.sugang_helper.domain.user.response;

import bhoon.sugang_helper.domain.user.entity.User;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(description = "사용자 프로필 정보 응답 DTO")
public class UserResponse {
    @Schema(description = "사용자 ID", example = "1")
    private Long id;

    @Schema(description = "이메일", example = "user@example.com")
    private String email;

    @Schema(description = "이름", example = "홍길동")
    private String name;

    @Schema(description = "역할 (USER, ADMIN)", example = "USER")
    private String role;

    public static UserResponse from(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .name(user.getName())
                .role(user.getRole().name())
                .build();
    }
}
