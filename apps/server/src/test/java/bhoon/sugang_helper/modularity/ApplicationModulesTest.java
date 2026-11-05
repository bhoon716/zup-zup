package bhoon.sugang_helper.modularity;

import static org.assertj.core.api.Assertions.assertThat;

import bhoon.sugang_helper.SugangHelperApplication;
import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;

class ApplicationModulesTest {

    @Test
    void modulesHaveNoCyclesOrUndeclaredInternalDependencies() {
        // given
        ApplicationModules modules = ApplicationModules.of(SugangHelperApplication.class);

        // when & then
        modules.verify();
        assertThat(modules.stream().map(module -> module.getIdentifier().toString()))
                .containsExactlyInAnyOrder("academic", "admin", "audit", "campus", "common",
                        "dashboard", "identity", "planning", "seatalert", "support");
    }
}
