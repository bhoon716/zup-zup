package bhoon.sugang_helper.dashboard.controller;

import bhoon.sugang_helper.common.response.CommonResponse;
import bhoon.sugang_helper.dashboard.dto.response.DashboardSnapshotResponse;
import bhoon.sugang_helper.dashboard.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping
    public ResponseEntity<CommonResponse<DashboardSnapshotResponse>> getDashboardSnapshot() {
        DashboardSnapshotResponse response = dashboardService.getDashboardSnapshot();
        return CommonResponse.ok(response, "대시보드 스냅샷입니다.");
    }
}
