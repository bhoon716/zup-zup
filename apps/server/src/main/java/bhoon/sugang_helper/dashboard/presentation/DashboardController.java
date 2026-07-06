package bhoon.sugang_helper.dashboard.application;

import bhoon.sugang_helper.common.response.CommonResponse;
import bhoon.sugang_helper.dashboard.application.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/dashboard")
@RequiredArgsConstructor
@Tag(name = "Dashboard", description = "사용자 대시보드 조회 API")
public class DashboardController {

    private final DashboardService dashboardService;

    @Operation(summary = "대시보드 스냅샷 조회", description = "로그인한 사용자의 대시보드에 필요한 데이터를 한 번에 조회합니다.")
    @GetMapping
    public ResponseEntity<CommonResponse<DashboardSnapshotResponse>> getDashboardSnapshot() {
        DashboardSnapshotResponse response = dashboardService.getDashboardSnapshot();
        return CommonResponse.ok(response, "대시보드 스냅샷입니다.");
    }
}
