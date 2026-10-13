package bhoon.sugang_helper.common.health;

import java.time.Instant;
import java.time.LocalDateTime;

public record HealthCheckResponse(
        String status,
        String version,
        Instant buildTime,
        LocalDateTime timestamp) {
}
