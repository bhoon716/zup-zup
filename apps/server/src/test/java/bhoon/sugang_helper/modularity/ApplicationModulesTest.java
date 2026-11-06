package bhoon.sugang_helper.modularity;

import static org.assertj.core.api.Assertions.assertThat;

import bhoon.sugang_helper.SugangHelperApplication;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;

class ApplicationModulesTest {

    @Test
    @DisplayName("모듈은 순환·미선언 의존과 내부 접근 없이 열 개로 구성된다")
    void modulesHaveNoCyclesOrUndeclaredInternalDependencies() {
        // when
        ApplicationModules modules = ApplicationModules.of(SugangHelperApplication.class);

        // then
        modules.verify();
        assertThat(modules.stream().map(module -> module.getIdentifier().toString()))
                .containsExactlyInAnyOrder("academic", "admin", "audit", "campus", "common",
                        "dashboard", "identity", "planning", "seatalert", "support");
    }
}
