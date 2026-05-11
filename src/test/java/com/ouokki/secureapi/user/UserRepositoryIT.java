package com.ouokki.secureapi.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ouokki.secureapi.support.PostgresIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@PostgresIntegrationTest
@Testcontainers(disabledWithoutDocker = true)
class UserRepositoryIT {

  @Container
  static PostgreSQLContainer<?> postgres =
      new PostgreSQLContainer<>("postgres:16-alpine")
          .withDatabaseName("secureapi")
          .withUsername("secureapi")
          .withPassword("test-secret");

  @DynamicPropertySource
  static void registerPostgresProps(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", postgres::getJdbcUrl);
    registry.add("spring.datasource.username", postgres::getUsername);
    registry.add("spring.datasource.password", postgres::getPassword);
  }

  @Autowired private UserRepository userRepository;

  @Test
  void migrationsRunCleanlyAndCrudWorks() {
    User user = User.create("alice@example.com", "argon2hash");
    User saved = userRepository.save(user);

    assertThat(saved.getId()).isNotNull();
    assertThat(saved.getCreatedAt()).isNotNull();
    assertThat(saved.getUpdatedAt()).isNotNull();
  }

  @Test
  void findByEmailIsCaseInsensitive() {
    userRepository.save(User.create("Bob@Example.COM", "hash"));

    assertThat(userRepository.findByEmail("bob@example.com")).isPresent();
    assertThat(userRepository.findByEmail("BOB@EXAMPLE.COM")).isPresent();
  }

  @Test
  void duplicateEmailViolatesUniqueConstraint() {
    userRepository.save(User.create("charlie@example.com", "hash1"));

    assertThatThrownBy(
            () -> userRepository.saveAndFlush(User.create("CHARLIE@example.com", "hash2")))
        .isInstanceOf(DataIntegrityViolationException.class);
  }
}
