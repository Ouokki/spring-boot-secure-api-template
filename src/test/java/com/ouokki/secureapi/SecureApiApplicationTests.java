package com.ouokki.secureapi;

import com.ouokki.secureapi.auth.RefreshTokenRepository;
import com.ouokki.secureapi.security.JwtIssuer;
import com.ouokki.secureapi.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@TestPropertySource(
    properties = {
      // Disable all database auto-configurations so this smoke test runs without Docker.
      // @MockitoBean below satisfies beans that would normally need JPA.
      "spring.autoconfigure.exclude="
          + "org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration,"
          + "org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration,"
          + "org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration,"
          + "org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration,"
          + "org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration"
    })
class SecureApiApplicationTests {

  // Satisfies AuthService's UserRepository dependency without a real datasource.
  @MockitoBean private UserRepository userRepository;

  // Prevents JwtIssuer from reading PEM files that don't exist outside dev/CI.
  @MockitoBean private JwtIssuer jwtIssuer;

  @MockitoBean private RefreshTokenRepository refreshTokenRepository;

  @Test
  void contextLoads() {}
}
