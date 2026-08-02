package bhoon.sugang_helper.common.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class MigrationValidationTaskConfigurationTest {

    @Test
    void defaultTestExcludesMigrationValidationAndDedicatedTaskSeparatesReports() throws IOException {
        String buildScript = Files.readString(Path.of("build.gradle"));

        assertThat(buildScript)
                .contains("excludeTags 'manual', 'performance', 'migration'")
                .contains("tasks.register('migrationTest', Test)")
                .contains("includeTags 'migration'")
                .contains("reports.html.outputLocation = layout.buildDirectory.dir('reports/tests/migrationTest')")
                .contains("reports.junitXml.outputLocation = layout.buildDirectory.dir('test-results/migrationTest')");
    }

    @Test
    void migrationValidationIsTaggedAndFailsClearlyWithoutDocker() throws IOException {
        String migrationTest = Files.readString(Path.of(
                "src/test/java/bhoon/sugang_helper/common/config/FlywayMigrationValidationTest.java"));

        assertThat(migrationTest)
                .contains("@Tag(\"migration\")")
                .contains("@BeforeAll")
                .contains("Docker is required for migrationTest")
                .contains("IllegalStateException")
                .doesNotContain("Assumptions.assumeTrue");
    }

    @Test
    void dockerPreflightFailsInsteadOfSkippingMigrationValidation() {
        assertThatIllegalStateException().isThrownBy(() -> FlywayMigrationValidationTest.requireDocker(() -> false))
                .withMessageContaining("Docker is required for migrationTest");
    }
}
