package com.ouokki.secureapi;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@TestPropertySource(
    properties = {
      // Disable database auto-configurations for this smoke test.
      // Integration tests that need a real DB use @DynamicPropertySource + Testcontainers.
      "spring.autoconfigure.exclude="
          + "org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration,"
          + "org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration,"
          + "org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration"
    })
class SecureApiApplicationTests {

  @Test
  void contextLoads() {}
}
