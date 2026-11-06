package bhoon.sugang_helper.common.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class MigrationValidationTaskConfigurationTest {

    @Test
    @DisplayName("기본 테스트는 마이그레이션 검증을 제외하고 전용 작업이 보고서를 분리한다")
    void defaultTestExcludesMigrationValidationAndDedicatedTaskSeparatesReports() throws IOException {
        // given
        String buildScript = Files.readString(Path.of("build.gradle"));

        // when & then
        assertThat(buildScript)
                .contains("excludeTags 'manual', 'performance', 'migration'")
                .contains("tasks.register('migrationTest', Test)")
                .contains("includeTags 'migration'")
                .contains("reports.html.outputLocation = layout.buildDirectory.dir('reports/tests/migrationTest')")
                .contains("reports.junitXml.outputLocation = layout.buildDirectory.dir('test-results/migrationTest')");
    }

    @Test
    @DisplayName("마이그레이션 검증은 태그로 구분하고 Docker가 없으면 명확히 실패한다")
    void migrationValidationIsTaggedAndFailsClearlyWithoutDocker() throws IOException {
        // given
        String migrationTest = Files.readString(Path.of(
                "src/test/java/bhoon/sugang_helper/common/config/FlywayMigrationValidationTest.java"));

        // when & then
        assertThat(migrationTest)
                .contains("@Tag(\"migration\")")
                .contains("@BeforeAll")
                .contains("Docker is required for migrationTest")
                .contains("IllegalStateException")
                .doesNotContain("Assumptions.assumeTrue");
    }

    @Test
    @DisplayName("Docker 사전 확인 실패 시 마이그레이션 검증을 건너뛰지 않는다")
    void dockerPreflightFailsInsteadOfSkippingMigrationValidation() {
        // when & then
        assertThatIllegalStateException().isThrownBy(() -> FlywayMigrationValidationTest.requireDocker(() -> false))
                .withMessageContaining("Docker is required for migrationTest");
    }
}
