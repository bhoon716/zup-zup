package bhoon.sugang_helper.common.health;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.time.LocalDateTime;

@Schema(description = "헬스 체크 응답")
public record HealthCheckResponse(
        @Schema(description = "상태", example = "UP") String status,
        @Schema(description = "버전", example = "0.0.1-SNAPSHOT") String version,
        @Schema(description = "빌드 시간") Instant buildTime,
        @Schema(description = "응답 시간", example = "2024-01-01T12:00:00") LocalDateTime timestamp) {
}
