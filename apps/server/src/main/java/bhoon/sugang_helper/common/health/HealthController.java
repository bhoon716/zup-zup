package bhoon.sugang_helper.common.health;

import bhoon.sugang_helper.common.response.CommonResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.info.BuildProperties;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
@Tag(name = "System", description = "시스템 관련 API")
public class HealthController {

    private final BuildProperties buildProperties;

    @GetMapping("/health")
    @Operation(summary = "헬스 체크", description = "서버 상태를 확인합니다.")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "요청 성공", content = @Content(mediaType = "application/json", examples = @ExampleObject(name = "헬스 체크 성공 예시", value = """
            {
                "status": "UP",
                "version": "1.0.0",
                "buildTime": "2025-12-12T10:00:00Z",
                "timestamp": "2025-12-12T10:00:00.123"
            }
            """)))
    public ResponseEntity<CommonResponse<HealthCheckResponse>> checkHealth() {
        HealthCheckResponse response = new HealthCheckResponse(
                "UP",
                buildProperties.getVersion(),
                buildProperties.getTime(),
                LocalDateTime.now());
        log.info("Health check 통과");
        return CommonResponse.ok(response, "Health check 통과");
    }
}
