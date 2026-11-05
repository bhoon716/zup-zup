package bhoon.sugang_helper.modularity;

import bhoon.sugang_helper.SugangHelperApplication;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;
import org.springframework.modulith.docs.Documenter;

@Tag("modulith-docs")
class ApplicationModuleDocumentationTest {

    @Test
    void documentsTheVerifiedModuleModel() {
        ApplicationModules modules = ApplicationModules.of(SugangHelperApplication.class);

        modules.verify();

        new Documenter(modules, Documenter.Options.defaults().withOutputFolder("build/spring-modulith"))
                .writeDocumentation()
                .writeModuleMetadata();
    }
}
