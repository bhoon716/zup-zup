package bhoon.sugang_helper.admin.presentation;

import bhoon.sugang_helper.dday.presentation.DdaySettingRequest;
import bhoon.sugang_helper.dday.presentation.DdaySettingResponse;
import bhoon.sugang_helper.dday.application.DdaySettingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@SecurityRequirement(name = "Cookie")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin Ddays", description = "어드민 D-Day 설정 관리 API")
public class AdminDdaySettingController {

    private final DdaySettingService ddaySettingService;

    @Operation(summary = "모든 D-Day 설정 목록 조회", description = "어드민 페이지 표시용 전체 D-Day 목록을 조회합니다.")
    @GetMapping
    public ResponseEntity<List<DdaySettingResponse>> getAllDdays() {
        return ResponseEntity.ok(ddaySettingService.getAllDdays());
    }

    @Operation(summary = "신규 D-Day 설정 생성")
    @PostMapping
    public ResponseEntity<DdaySettingResponse> createDday(@RequestBody @Valid DdaySettingRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ddaySettingService.createDday(request));
    }

    @Operation(summary = "기존 D-Day 설정 수정")
    @PutMapping("/{id}")
    public ResponseEntity<DdaySettingResponse> updateDday(@PathVariable Long id,
                                                         @RequestBody @Valid DdaySettingRequest request) {
        return ResponseEntity.ok(ddaySettingService.updateDday(id, request));
    }

    @Operation(summary = "D-Day 설정 삭제")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteDday(@PathVariable Long id) {
        ddaySettingService.deleteDday(id);
        return ResponseEntity.noContent().build();
    }
}
