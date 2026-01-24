package bhoon.sugang_helper.domain.user.controller;

import bhoon.sugang_helper.common.response.CommonResponse;
import bhoon.sugang_helper.domain.user.response.UserResponse;
import bhoon.sugang_helper.domain.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Tag(name = "User", description = "사용자 관련 API")
public class UserController {

    private final UserService userService;

    @Operation(summary = "내 프로필 조회", description = "현재 로그인한 사용자의 프로필 정보를 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "프로필 조회 성공", content = @Content(schema = @Schema(implementation = CommonResponse.class), examples = @ExampleObject(value = """
                    {
                      "code": "SUCCESS",
                      "message": "사용자 프로필 정보입니다.",
                      "data": {
                        "id": 1,
                        "email": "user@example.com",
                        "name": "홍길동",
                        "role": "USER"
                      }
                    }
                    """)))
    })
    @GetMapping("/me")
    public ResponseEntity<CommonResponse<UserResponse>> getMyProfile() {
        UserResponse response = userService.getMyProfile();
        return CommonResponse.ok(response, "사용자 프로필 정보입니다.");
    }

    @Operation(summary = "회원 탈퇴", description = "사용자 계정을 삭제합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "탈퇴 성공", content = @Content(schema = @Schema(implementation = CommonResponse.class), examples = @ExampleObject(value = """
                    {
                      "code": "SUCCESS",
                      "message": "회원 탈퇴가 완료되었습니다.",
                      "data": null
                    }
                    """)))
    })
    @DeleteMapping("/me")
    public ResponseEntity<CommonResponse<Void>> withdraw() {
        userService.withdraw();
        return CommonResponse.ok(null, "회원 탈퇴가 완료되었습니다.");
    }
}
