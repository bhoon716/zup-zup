package bhoon.sugang_helper.dashboard.controller;

import bhoon.sugang_helper.dashboard.dto.response.DashboardSnapshotResponse;
import bhoon.sugang_helper.dashboard.dto.response.LandingStatsResponse;
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

    @GetMapping("/landing-stats")
    public ResponseEntity<LandingStatsResponse> getLandingStats() {
        return ResponseEntity.ok(dashboardService.getLandingStats());
    }

    @GetMapping
    public ResponseEntity<DashboardSnapshotResponse> getDashboardSnapshot() {
        DashboardSnapshotResponse response = dashboardService.getDashboardSnapshot();
        return ResponseEntity.ok(response);
    }
}
