package bhoon.sugang_helper.common.health;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import bhoon.sugang_helper.common.response.CommonResponse;
import java.time.Instant;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthEndpoint;
import org.springframework.boot.info.BuildProperties;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

class HealthControllerTest {

    private static final Instant BUILD_TIME = Instant.parse("2026-01-25T01:00:00Z");
    private static final String READINESS_GROUP = "readiness";

    private BuildProperties buildProperties;
    private HealthEndpoint healthEndpoint;
    private HealthController healthController;

    @BeforeEach
    void setUp() {
        buildProperties = mock(BuildProperties.class);
        healthEndpoint = mock(HealthEndpoint.class);
        healthController = new HealthController(buildProperties, healthEndpoint);

        given(buildProperties.getVersion()).willReturn("test-version");
        given(buildProperties.getTime()).willReturn(BUILD_TIME);
    }

    @Test
    void externalHealthReturnsOkWhenReadinessIsUp() {
        // given
        given(healthEndpoint.healthForPath(READINESS_GROUP)).willReturn(Health.up().build());

        // when
        ResponseEntity<CommonResponse<HealthCheckResponse>> response = healthController.checkHealth();

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getData().status()).isEqualTo("UP");
        assertThat(response.getBody().getData().version()).isEqualTo("test-version");
        verify(healthEndpoint).healthForPath(READINESS_GROUP);
    }

    @ParameterizedTest(name = "{0} 장애 시 외부 health endpoint는 실패한다")
    @MethodSource("dependencyFailures")
    void externalHealthReturnsServiceUnavailableWhenReadinessIsDown(
            String dependency,
            Health readinessHealth) {
        // given
        given(healthEndpoint.healthForPath(READINESS_GROUP)).willReturn(readinessHealth);

        // when
        ResponseEntity<CommonResponse<HealthCheckResponse>> response = healthController.checkHealth();

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getData().status()).isEqualTo("DOWN");
        assertThat(response.getBody().getData().version()).isEqualTo("test-version");
        assertThat(readinessHealth.getDetails()).containsKey(dependency);
        verify(healthEndpoint).healthForPath(READINESS_GROUP);
    }

    private static Stream<Arguments> dependencyFailures() {
        return Stream.of(
                Arguments.of("db", Health.down().withDetail("db", "connection refused").build()),
                Arguments.of("redis", Health.down().withDetail("redis", "connection refused").build()));
    }
}
