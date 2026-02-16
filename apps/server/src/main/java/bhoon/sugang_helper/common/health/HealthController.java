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
import org.springframework.boot.info.BuildProperties;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
@Tag(name = "Health", description = "헬스체크 API")
public class HealthController {

    private final BuildProperties buildProperties;

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
                    """)))
    })
    @GetMapping("/health")
    public ResponseEntity<CommonResponse<HealthCheckResponse>> checkHealth() {
        HealthCheckResponse response = new HealthCheckResponse(
                "UP",
                buildProperties.getVersion(),
                buildProperties.getTime(),
                LocalDateTime.now());
        log.info("헬스 체크 통과");
        return CommonResponse.ok(response, "헬스 체크 통과");
    }
}
