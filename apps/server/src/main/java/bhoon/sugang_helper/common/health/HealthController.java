package bhoon.sugang_helper.common.health;

import bhoon.sugang_helper.common.response.CommonResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.health.HealthComponent;
import org.springframework.boot.actuate.health.HealthEndpoint;
import org.springframework.boot.actuate.health.Status;
import org.springframework.boot.info.BuildProperties;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
@Tag(name = "Health", description = "헬스체크 API")
public class HealthController {

    private static final String READINESS_GROUP = "readiness";

    private final BuildProperties buildProperties;
    private final HealthEndpoint healthEndpoint;

    @Operation(summary = "헬스 체크", description = "서버 상태를 확인합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "헬스 체크 성공", content = @Content(schema = @Schema(implementation = CommonResponse.class), examples = @ExampleObject(value = """
                    {
                      "code": "SUCCESS",
                      "message": "헬스 체크 통과",
                      "data": {
                        "status": "UP",
                        "version": "0.0.1",
                        "buildTime": "2026-01-25T01:00:00Z",
                        "timestamp": "2026-01-25T01:00:00"
                      }
                    }
                    """))),
            @ApiResponse(responseCode = "503", description = "DB 또는 Redis readiness 실패", content = @Content(schema = @Schema(implementation = CommonResponse.class)))
    })
    @GetMapping("/health")
    public ResponseEntity<CommonResponse<HealthCheckResponse>> checkHealth() {
        HealthComponent readiness = healthEndpoint.healthForPath(READINESS_GROUP);
        boolean isReady = readiness != null && Status.UP.equals(readiness.getStatus());
        String status = isReady ? Status.UP.getCode() : Status.DOWN.getCode();
        HealthCheckResponse response = new HealthCheckResponse(
                status,
                buildProperties.getVersion(),
                buildProperties.getTime(),
                LocalDateTime.now());
        if (isReady) {
            log.info("Health check passed");
        } else {
            String readinessStatus = readiness == null ? "UNKNOWN" : readiness.getStatus().getCode();
            log.warn("Health check failed: readinessStatus={}", readinessStatus);
        }
        HttpStatus httpStatus = isReady ? HttpStatus.OK : HttpStatus.SERVICE_UNAVAILABLE;
        String message = isReady ? "헬스 체크 통과" : "의존성 헬스 체크 실패";
        return ResponseEntity.status(httpStatus).body(CommonResponse.success(response, message));
    }
}
