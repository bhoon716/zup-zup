package bhoon.sugang_helper.common.health;

import bhoon.sugang_helper.common.dto.response.HealthCheckResponse;
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
public class HealthController {

    private static final String READINESS_GROUP = "readiness";

    private final BuildProperties buildProperties;
    private final HealthEndpoint healthEndpoint;

    @GetMapping("/health")
    public ResponseEntity<HealthCheckResponse> checkHealth() {
        HealthComponent readiness = healthEndpoint.healthForPath(READINESS_GROUP);
        boolean isReady = readiness != null && Status.UP.equals(readiness.getStatus());
        String status = isReady ? Status.UP.getCode() : Status.DOWN.getCode();
        HealthCheckResponse response = new HealthCheckResponse(
                status,
                buildProperties.getVersion(),
                buildProperties.getTime(),
                LocalDateTime.now());
        if (isReady) {
            logReady();
        } else {
            String readinessStatus = readiness == null ? "UNKNOWN" : readiness.getStatus().getCode();
            logNotReady(readinessStatus);
        }
        HttpStatus httpStatus = isReady ? HttpStatus.OK : HttpStatus.SERVICE_UNAVAILABLE;
        return ResponseEntity.status(httpStatus).body(response);
    }

    private void logReady() {
        log.atDebug().addKeyValue("event", "health_ready")
                .log("상태 확인 정상.");
    }

    private void logNotReady(String readinessStatus) {
        log.atWarn().addKeyValue("event", "health_not_ready")
                .addKeyValue("readinessStatus", readinessStatus)
                .log("상태 확인 실패.");
    }

}
