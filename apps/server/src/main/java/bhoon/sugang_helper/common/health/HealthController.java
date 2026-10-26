package bhoon.sugang_helper.common.health;

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
            log.debug("상태 확인 정상.");
        } else {
            String readinessStatus = readiness == null ? "UNKNOWN" : readiness.getStatus().getCode();
            log.warn("상태 확인 실패. readinessStatus={}", readinessStatus);
        }
        HttpStatus httpStatus = isReady ? HttpStatus.OK : HttpStatus.SERVICE_UNAVAILABLE;
        return ResponseEntity.status(httpStatus).body(response);
    }
}
