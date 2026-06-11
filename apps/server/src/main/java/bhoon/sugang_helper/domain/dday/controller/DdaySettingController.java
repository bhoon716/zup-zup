package bhoon.sugang_helper.domain.dday.controller;

import bhoon.sugang_helper.domain.dday.response.DdaySettingResponse;
import bhoon.sugang_helper.domain.dday.service.DdaySettingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/ddays")
@RequiredArgsConstructor
@Tag(name = "Ddays", description = "D-Day 조회 API (유저용)")
public class DdaySettingController {

    private final DdaySettingService ddaySettingService;

    @Operation(summary = "활성 D-Day 단 건 조회", description = "현재 시간 기준 가장 가까운 미래의 D-Day 일정을 조회합니다.")
    @GetMapping("/active")
    public ResponseEntity<DdaySettingResponse> getActiveDday() {
        return ddaySettingService.getActiveDday()
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.noContent().build());
    }
}
