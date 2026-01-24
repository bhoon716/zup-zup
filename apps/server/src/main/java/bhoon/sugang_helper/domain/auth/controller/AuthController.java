package bhoon.sugang_helper.domain.auth.controller;

import bhoon.sugang_helper.common.response.CommonResponse;
import bhoon.sugang_helper.domain.auth.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Auth", description = "인증 관련 API")
public class AuthController {

    private final AuthService authService;

    @Operation(summary = "토큰 재발급", description = "Refresh Token을 사용하여 Access Token을 재발급합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "토큰 재발급 성공", content = @Content(schema = @Schema(implementation = CommonResponse.class), examples = @ExampleObject(value = """
                    {
                      "code": "SUCCESS",
                      "message": "Reissue Success",
                      "data": "new-access-token"
                    }
                    """)))
    })
    @PostMapping("/refresh")
    public ResponseEntity<CommonResponse<String>> reissue(HttpServletRequest request, HttpServletResponse response) {
        String newAccessToken = authService.reissue(request, response);
        return ResponseEntity.ok(CommonResponse.success(newAccessToken, "Reissue Success"));
    }

    @Operation(summary = "로그아웃", description = "사용자를 로그아웃 처리하고 토큰을 무효화합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "로그아웃 성공", content = @Content(schema = @Schema(implementation = CommonResponse.class), examples = @ExampleObject(value = """
                    {
                      "code": "SUCCESS",
                      "message": "Logout Success",
                      "data": null
                    }
                    """)))
    })
    @PostMapping("/logout")
    public ResponseEntity<CommonResponse<Void>> logout(HttpServletRequest request, HttpServletResponse response) {
        authService.logout(request, response);
        return ResponseEntity.ok(CommonResponse.success(null, "Logout Success"));
    }
}
