package bhoon.sugang_helper.common.dto.response;

import java.time.Instant;
import java.time.LocalDateTime;

public record HealthCheckResponse(
        String status,
        String version,
        Instant buildTime,
        LocalDateTime timestamp) {

    public static HealthCheckResponse of(String status, String version, Instant buildTime, LocalDateTime timestamp) {
        return new HealthCheckResponse(status, version, buildTime, timestamp);
    }
}
