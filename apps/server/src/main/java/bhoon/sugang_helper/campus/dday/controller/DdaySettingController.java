package bhoon.sugang_helper.campus.dday.controller;

import bhoon.sugang_helper.campus.dday.service.DdaySettingService;
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
                .<ResponseEntity<?>>map(dday -> ResponseEntity.ok(dday))
                .orElseGet(() -> ResponseEntity.noContent().build());
    }
}
