package com.ouokki.secureapi.arch;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.Architectures.layeredArchitecture;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class ArchitectureTest {

  static JavaClasses classes;

  @BeforeAll
  static void importClasses() {
    classes =
        new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages("com.ouokki.secureapi");
  }

  @Test
  void layeredArchitectureIsRespected() {
    layeredArchitecture()
        .consideringOnlyDependenciesInLayers()
        .layer("Web").definedBy("..web..")
        .layer("Auth").definedBy("..auth..")
        .layer("User").definedBy("..user..")
        .layer("Security").definedBy("..security..")
        .layer("Audit").definedBy("..audit..")
        .layer("RateLimit").definedBy("..ratelimit..")
        .layer("Observability").definedBy("..observability..")
        .whereLayer("Web").mayNotBeAccessedByAnyLayer()
        .whereLayer("Auth").mayOnlyBeAccessedByLayers("Web")
        .whereLayer("User").mayOnlyBeAccessedByLayers("Web", "Auth", "Security")
        .whereLayer("Security").mayOnlyBeAccessedByLayers("Web", "Auth")
        .check(classes);
  }

  @Test
  void repositoriesAreOnlyInPersistencePackages() {
    classes()
        .that()
        .haveNameMatching(".*Repository")
        .should()
        .resideInAnyPackage("..auth..", "..user..")
        .check(classes);
  }

  @Test
  void entitiesAreOnlyInPersistencePackages() {
    classes()
        .that()
        .areAnnotatedWith(jakarta.persistence.Entity.class)
        .should()
        .resideInAnyPackage("..auth..", "..user..")
        .check(classes);
  }

  @Test
  void controllersOnlyInWebAndAuthPackages() {
    classes()
        .that()
        .areAnnotatedWith(org.springframework.web.bind.annotation.RestController.class)
        .should()
        .resideInAnyPackage("..web..", "..auth..")
        .check(classes);
  }

  @Test
  void noDirectJdbcUsageOutsideRepositories() {
    noClasses()
        .that()
        .resideOutsideOfPackage("..auth..")
        .and()
        .resideOutsideOfPackage("..user..")
        .should()
        .dependOnClassesThat()
        .resideInAnyPackage("javax.sql..", "java.sql..")
        .check(classes);
  }
}
