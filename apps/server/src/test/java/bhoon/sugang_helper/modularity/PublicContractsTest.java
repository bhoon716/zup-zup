package bhoon.sugang_helper.modularity;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static org.assertj.core.api.Assertions.assertThat;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import jakarta.persistence.Entity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.modulith.NamedInterface;

class PublicContractsTest {

    @Test
    @DisplayName("공개 계약은 업무 구현 타입이나 엔티티를 노출하지 않는다")
    void publicContractsDoNotExposeBusinessImplementationTypes() {
        // given
        JavaClasses application = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages("bhoon.sugang_helper");

        // when & then
        assertThat(application.stream().filter(type -> type.getPackageName().contains(".api."))).isNotEmpty();
        assertThat(application.stream().filter(type -> !type.getName().endsWith(".package-info"))
                .filter(type -> type.isAnnotatedWith(NamedInterface.class))).isEmpty();

        noClasses().that().resideInAnyPackage("..api..", "..events..")
                .should().dependOnClassesThat()
                .resideInAnyPackage("bhoon.sugang_helper..entity..", "bhoon.sugang_helper..repository..",
                        "bhoon.sugang_helper..service..")
                .check(application);
        noClasses().that().resideInAnyPackage("..api..", "..events..")
                .should().beAnnotatedWith(Entity.class).check(application);
    }
}
