package bhoon.sugang_helper.admin.application;

import bhoon.sugang_helper.common.response.CommonResponse;
import bhoon.sugang_helper.admin.application.AdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
@Tag(name = "Admin", description = "관리자 전용 API")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

  private final AdminService adminService;

  /**
   * 관리자 페이지에서 필요한 개요와 크롤링 타겟 정보를 한 번에 조회합니다.
   */
  @Operation(summary = "관리자 대시보드 스냅샷", description = "대시보드 렌더링에 필요한 개요와 크롤링 타겟을 함께 조회합니다.")
  @GetMapping("/dashboard")
  public ResponseEntity<CommonResponse<AdminDashboardSnapshotResponse>> getDashboardSnapshot() {
    AdminDashboardSnapshotResponse response = adminService.getDashboardSnapshot();
    return CommonResponse.ok(response, "관리자 대시보드 스냅샷 정보입니다.");
  }

  /**
   * 현재 관리자의 알림 설정에 맞춰 테스트 알림을 발송합니다.
   */
  @Operation(summary = "알림 테스트 전송", description = "현재 로그인한 관리자의 설정된 채널로 테스트 알림을 전송합니다.")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "테스트 알림 요청 성공", content = @Content(schema = @Schema(implementation = CommonResponse.class), examples = @ExampleObject(value = """
          {
            "code": "SUCCESS",
            "message": "테스트 알림이 성공적으로 요청되었습니다.",
            "data": null
          }
          """))),
      @ApiResponse(responseCode = "400", description = "활성화된 알림 채널 없음", content = @Content(schema = @Schema(implementation = CommonResponse.class), examples = @ExampleObject(value = """
          {
            "code": "INVALID_INPUT",
            "message": "활성화된 알림 채널이 없습니다. 설정에서 알림을 활성화해주세요.",
            "data": null
          }
          """)))
  })
  @PostMapping("/notifications/test")
  public ResponseEntity<CommonResponse<Void>> sendTestNotification() {
    adminService.sendTestNotification();
    return CommonResponse.ok(null, "테스트 알림이 성공적으로 요청되었습니다.");
  }
}
