package bhoon.sugang_helper.domain.user.controller;

import bhoon.sugang_helper.common.response.CommonResponse;
import bhoon.sugang_helper.domain.user.request.EmailRequest;
import bhoon.sugang_helper.domain.user.request.EmailVerificationRequest;
import bhoon.sugang_helper.domain.user.request.OnboardingRequest;
import bhoon.sugang_helper.domain.user.request.UserSettingsRequest;
import bhoon.sugang_helper.domain.user.request.UserUpdateRequest;
import bhoon.sugang_helper.domain.user.response.UserResponse;
import bhoon.sugang_helper.domain.user.service.DiscordOAuthService;
import bhoon.sugang_helper.domain.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Tag(name = "User", description = "사용자 관련 API")
public class UserController {

  private final UserService userService;
  private final DiscordOAuthService discordOAuthService;

  @Value("${app.oauth2.authorized-redirect-uri}")
  private String frontendBaseUrl;

  /**
   * 신규 가입 유저의 초기 설정(알림 이메일 등)을 저장하고 온보딩 상태를 완료로 변경합니다.
   */
  @Operation(summary = "온보딩 완료", description = "신규 가입 유저의 초기 설정(알림 이메일 등)을 저장하고 온보딩 상태를 완료로 변경합니다.")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "온보딩 완료 성공", content = @Content(schema = @Schema(implementation = CommonResponse.class)))
  })
  @PostMapping("/onboard")
  public ResponseEntity<CommonResponse<UserResponse>> completeOnboarding(
      @Valid @RequestBody OnboardingRequest request) {
    UserResponse response = userService.completeOnboarding(request);
    return CommonResponse.ok(response, "온보딩 설정이 완료되었습니다.");
  }

  /**
   * 입력한 이메일로 인증 코드를 전송합니다.
   */
  @Operation(summary = "이메일 인증 코드 전송", description = "입력한 이메일로 인증 코드를 전송합니다.")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "코드 전송 성공", content = @Content(schema = @Schema(implementation = CommonResponse.class), examples = @ExampleObject(value = """
          {
            "code": "SUCCESS",
            "message": "인증 코드가 전송되었습니다. 이메일을 확인해주세요.",
            "data": null
          }
          """)))
  })
  @PostMapping("/email/code")
  public ResponseEntity<CommonResponse<Void>> sendVerificationCode(
      @Valid @RequestBody EmailRequest request) {
    userService.sendVerificationCode(request);
    return CommonResponse.ok(null, "인증 코드가 전송되었습니다. 이메일을 확인해주세요.");
  }

  /**
   * 이메일로 전송된 인증 코드를 검증합니다.
   */
  @Operation(summary = "이메일 인증", description = "이메일로 전송된 인증 코드를 검증합니다.")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "이메일 인증 성공", content = @Content(schema = @Schema(implementation = CommonResponse.class), examples = @ExampleObject(value = """
          {
            "code": "SUCCESS",
            "message": "이메일 인증이 완료되었습니다.",
            "data": null
          }
          """))),
      @ApiResponse(responseCode = "400", description = "잘못된 인증 코드", content = @Content(schema = @Schema(implementation = CommonResponse.class), examples = @ExampleObject(value = """
          {
            "code": "INVALID_VERIFICATION_CODE",
            "message": "인증 코드가 일치하지 않거나 만료되었습니다.",
            "data": null
          }
          """)))
  })
  @PostMapping("/email/verify")
  public ResponseEntity<CommonResponse<Void>> verifyEmail(
      @Valid @RequestBody EmailVerificationRequest request) {
    userService.verifyEmail(request);
    return CommonResponse.ok(null, "이메일 인증이 완료되었습니다.");
  }

  /**
   * 현재 로그인한 사용자의 프로필 정보를 조회합니다.
   */
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

  /**
   * 현재 로그인한 사용자의 프로필 정보를 수정합니다.
   */
  @Operation(summary = "내 프로필 수정", description = "현재 로그인한 사용자의 프로필 정보를 수정합니다.")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "프로필 수정 성공", content = @Content(schema = @Schema(implementation = CommonResponse.class), examples = @ExampleObject(value = """
          {
            "code": "SUCCESS",
            "message": "사용자 프로필 정보가 수정되었습니다.",
            "data": {
              "id": 1,
              "email": "user@example.com",
              "name": "홍길순",
              "role": "USER"
            }
          }
          """)))
  })
  @PatchMapping("/me")
  public ResponseEntity<CommonResponse<UserResponse>> updateProfile(@Valid @RequestBody UserUpdateRequest request) {
    UserResponse response = userService.updateProfile(request.getName());
    return CommonResponse.ok(response, "사용자 프로필 정보가 수정되었습니다.");
  }

  /**
   * 현재 로그인한 사용자의 알림 수신 설정을 수정합니다.
   */
  @Operation(summary = "사용자 알림 설정 수정", description = "현재 로그인한 사용자의 알림 수신 설정을 수정합니다.")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "설정 수정 성공", content = @Content(schema = @Schema(implementation = CommonResponse.class), examples = @ExampleObject(value = """
          {
            "code": "SUCCESS",
            "message": "사용자 알림 설정이 수정되었습니다.",
            "data": {
              "id": 1,
              "email": "user@jbnu.ac.kr",
              "emailEnabled": true,
              "discordEnabled": false
            }
          }
          """)))
  })
  @PatchMapping("/settings")
  public ResponseEntity<CommonResponse<UserResponse>> updateSettings(
      @Valid @RequestBody UserSettingsRequest request) {
    UserResponse response = userService.updateSettings(request);
    return CommonResponse.ok(response, "사용자 알림 설정이 수정되었습니다.");
  }

  /**
   * 현재 로그인한 사용자의 활성화된 채널로 테스트 알림을 발송합니다.
   */
  @Operation(summary = "사용자 알림 테스트 발송", description = "현재 로그인한 사용자의 활성화된 채널로 테스트 알림을 발송합니다. 10초 쿨타임이 적용됩니다.")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "알림 테스트 발송 성공", content = @Content(schema = @Schema(implementation = CommonResponse.class), examples = @ExampleObject(value = """
          {
            "code": "SUCCESS",
            "message": "알림 테스트를 전송했습니다.",
            "data": null
          }
          """))),
      @ApiResponse(responseCode = "400", description = "활성화된 채널 없음", content = @Content(schema = @Schema(implementation = CommonResponse.class), examples = @ExampleObject(value = """
          {
            "code": "INVALID_INPUT",
            "message": "활성화된 알림 채널이 없습니다. 설정에서 알림을 활성화해주세요.",
            "data": null
          }
          """))),
      @ApiResponse(responseCode = "429", description = "쿨타임 제한", content = @Content(schema = @Schema(implementation = CommonResponse.class), examples = @ExampleObject(value = """
          {
            "code": "TOO_MANY_REQUESTS",
            "message": "알림 테스트는 10초에 한 번만 가능합니다. 잠시 후 다시 시도해주세요.",
            "data": null
          }
          """)))
  })
  @PostMapping("/notifications/test")
  public ResponseEntity<CommonResponse<Void>> sendTestNotification() {
    userService.sendTestNotification();
    return CommonResponse.ok(null, "알림 테스트를 전송했습니다.");
  }

  /**
   * 사용자 계정을 삭제하고 회원 탈퇴를 처리합니다.
   */
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

  /**
   * 현재 계정에 연동된 디스코드 계정을 해제합니다.
   */
  @Operation(summary = "디스코드 연동 해제", description = "현재 계정에 연동된 디스코드 계정을 해제합니다.")
  @DeleteMapping("/me/discord")
  public ResponseEntity<CommonResponse<Void>> unlinkDiscord() {
    userService.unlinkDiscord();
    return CommonResponse.ok(null, "디스코드 연동이 해제되었습니다.");
  }

  /**
   * 디스코드 인증 후 리다이렉트되어 연동을 완료하는 콜백 엔드포인트입니다.
   */
  @Operation(summary = "디스코드 OAuth2 콜백", description = "디스코드 인증 후 리다이렉트되어 연동을 완료합니다.")
  @GetMapping("/discord/callback")
  public ResponseEntity<Void> discordCallback(@RequestParam String code,
      @RequestParam(required = false) String state) {
    String redirectPath = resolveDiscordRedirectPath(state);

    try {
      linkDiscordAccount(code);
      return buildDiscordRedirectResponse(redirectPath, "success");
    } catch (Exception e) {
      log.warn("[디스코드] OAuth 콜백 처리에 실패했습니다. state={}, reason={}", state, e.getMessage());
      return buildDiscordRedirectResponse(redirectPath, "error");
    }
  }

  /**
   * 디스코드 인증 코드를 사용하여 계정을 연동합니다.
   */
  private void linkDiscordAccount(String code) {
    String accessToken = discordOAuthService.exchangeCodeForToken(code);
    String discordId = discordOAuthService.getDiscordUserId(accessToken);
    userService.linkDiscordId(discordId);
  }

  /**
   * 디스코드 연동 시도 시의 상태값에 따른 리다이렉트 경로를 결정합니다.
   */
  private String resolveDiscordRedirectPath(String state) {
    if ("onboarding".equals(state)) {
      return "/onboarding";
    }
    return "/settings";
  }

  /**
   * 디스코드 연동 결과와 경로를 포함한 리다이렉트 응답을 생성합니다.
   */
  private ResponseEntity<Void> buildDiscordRedirectResponse(String redirectPath, String result) {
    return ResponseEntity.status(302)
        .header("Location", frontendBaseUrl + redirectPath + "?discord=" + result)
        .build();
  }
}
