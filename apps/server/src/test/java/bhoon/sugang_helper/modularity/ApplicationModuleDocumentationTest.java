package bhoon.sugang_helper.modularity;

import bhoon.sugang_helper.SugangHelperApplication;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;
import org.springframework.modulith.docs.Documenter;

@Tag("modulith-docs")
class ApplicationModuleDocumentationTest {

    @Test
    @DisplayName("검증한 모듈 모델로 문서와 메타데이터를 생성한다")
    void documentsTheVerifiedModuleModel() {
        // given
        ApplicationModules modules = ApplicationModules.of(SugangHelperApplication.class);

        // when & then
        modules.verify();

        new Documenter(modules, Documenter.Options.defaults().withOutputFolder("build/spring-modulith"))
                .writeDocumentation()
                .writeModuleMetadata();
    }
}
