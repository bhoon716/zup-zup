package bhoon.sugang_helper.campus.dday.controller;

import bhoon.sugang_helper.campus.dday.service.DdaySettingService;
import bhoon.sugang_helper.common.response.CommonResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/ddays")
@RequiredArgsConstructor
public class DdaySettingController {

    private final DdaySettingService ddaySettingService;

    @GetMapping("/active")
    public ResponseEntity<?> getActiveDday() {
        return ddaySettingService.getActiveDday()
                .<ResponseEntity<?>>map(dday -> CommonResponse.ok(dday, "활성 D-Day를 조회했습니다."))
                .orElseGet(() -> ResponseEntity.noContent().build());
    }
}
