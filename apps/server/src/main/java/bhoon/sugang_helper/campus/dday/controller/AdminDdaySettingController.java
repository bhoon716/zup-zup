package bhoon.sugang_helper.campus.dday.controller;

import bhoon.sugang_helper.campus.dday.dto.request.DdaySettingRequest;
import bhoon.sugang_helper.campus.dday.dto.response.DdaySettingResponse;
import bhoon.sugang_helper.campus.dday.service.DdaySettingService;
import bhoon.sugang_helper.common.response.CommonResponse;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/ddays")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminDdaySettingController {

    private final DdaySettingService ddaySettingService;

    @GetMapping
    public ResponseEntity<CommonResponse<List<DdaySettingResponse>>> getAllDdays() {
        return CommonResponse.ok(ddaySettingService.getAllDdays(), "전체 D-Day 목록입니다.");
    }

    @PostMapping
    public ResponseEntity<CommonResponse<DdaySettingResponse>> createDday(
            @RequestBody @Valid DdaySettingRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(CommonResponse.success(ddaySettingService.createDday(request), "D-Day 설정을 생성했습니다."));
    }

    @PutMapping("/{id}")
    public ResponseEntity<CommonResponse<DdaySettingResponse>> updateDday(@PathVariable Long id,
                                                                          @RequestBody @Valid DdaySettingRequest request) {
        return CommonResponse.ok(ddaySettingService.updateDday(id, request), "D-Day 설정을 수정했습니다.");
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteDday(@PathVariable Long id) {
        ddaySettingService.deleteDday(id);
        return ResponseEntity.noContent().build();
    }
}
