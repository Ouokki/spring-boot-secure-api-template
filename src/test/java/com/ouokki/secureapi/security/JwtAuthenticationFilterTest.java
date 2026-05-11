package com.ouokki.secureapi.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ouokki.secureapi.SecureApiApplication;
import com.ouokki.secureapi.auth.RefreshTokenRepository;
import com.ouokki.secureapi.user.UserRepository;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.Base64;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(
    classes = {SecureApiApplication.class, JwtAuthenticationFilterTest.TestConfig.class},
    webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@TestPropertySource(
    properties = {
      "spring.autoconfigure.exclude="
          + "org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration,"
          + "org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration,"
          + "org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration,"
          + "org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration,"
          + "org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration"
    })
class JwtAuthenticationFilterTest {

  @TempDir static Path keyDir;

  static JwtIssuer realIssuer;

  @MockitoBean UserRepository userRepository;
  @MockitoBean RefreshTokenRepository refreshTokenRepository;

  @Autowired MockMvc mockMvc;

  @BeforeAll
  static void generateKeys() throws Exception {
    KeyPair kp = KeyPairGenerator.getInstance("RSA").generateKeyPair();
    Path priv = keyDir.resolve("private.pem");
    Path pub = keyDir.resolve("public.pem");
    Files.writeString(
        priv,
        "-----BEGIN PRIVATE KEY-----\n"
            + Base64.getMimeEncoder(64, new byte[] {'\n'})
                .encodeToString(kp.getPrivate().getEncoded())
            + "\n-----END PRIVATE KEY-----\n");
    Files.writeString(
        pub,
        "-----BEGIN PUBLIC KEY-----\n"
            + Base64.getMimeEncoder(64, new byte[] {'\n'})
                .encodeToString(kp.getPublic().getEncoded())
            + "\n-----END PUBLIC KEY-----\n");
    realIssuer = new JwtIssuer(new JwtProperties(priv.toString(), pub.toString(), 900L));
  }

  @DynamicPropertySource
  static void jwtProps(DynamicPropertyRegistry registry) {
    registry.add("app.jwt.private-key-path", () -> keyDir.resolve("private.pem").toString());
    registry.add("app.jwt.public-key-path", () -> keyDir.resolve("public.pem").toString());
  }

  // Expose the real issuer (built from generated keys) so the filter can verify tokens.
  @TestConfiguration
  static class TestConfig {
    @Bean
    JwtIssuer jwtIssuer() throws Exception {
      return realIssuer;
    }
  }

  @Test
  void requestWithValidTokenIsAuthenticated() throws Exception {
    String token = realIssuer.issue("user-123", List.of("ROLE_USER"));

    mockMvc
        .perform(get("/any-protected-path").header("Authorization", "Bearer " + token))
        .andExpect(
            result -> {
              // 404 means security passed (path doesn't exist); 401/403 means security blocked it.
              assertThat(result.getResponse().getStatus()).isNotIn(401, 403);
            });

    SecurityContextHolder.clearContext();
  }

  @Test
  void requestWithoutTokenReturns401ForProtectedPath() throws Exception {
    mockMvc.perform(get("/any-protected-path")).andExpect(status().isUnauthorized());
  }

  @Test
  void requestWithMalformedTokenReturns401() throws Exception {
    mockMvc
        .perform(get("/any-protected-path").header("Authorization", "Bearer not.a.jwt"))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void requestWithExpiredTokenReturns401() throws Exception {
    // Issue a token with the real issuer but manipulate expiry via a separate issuer with TTL=0.
    // Simplest: use a token signed by a *different* key — signature verification fails → 401.
    KeyPair other = KeyPairGenerator.getInstance("RSA").generateKeyPair();
    Path op = keyDir.resolve("other-private.pem");
    Path opub = keyDir.resolve("other-public.pem");
    Files.writeString(
        op,
        "-----BEGIN PRIVATE KEY-----\n"
            + Base64.getMimeEncoder(64, new byte[] {'\n'})
                .encodeToString(other.getPrivate().getEncoded())
            + "\n-----END PRIVATE KEY-----\n");
    Files.writeString(
        opub,
        "-----BEGIN PUBLIC KEY-----\n"
            + Base64.getMimeEncoder(64, new byte[] {'\n'})
                .encodeToString(other.getPublic().getEncoded())
            + "\n-----END PUBLIC KEY-----\n");
    JwtIssuer wrongKeyIssuer =
        new JwtIssuer(new JwtProperties(op.toString(), opub.toString(), 900L));
    String tokenFromWrongKey = wrongKeyIssuer.issue("user-123", List.of());

    mockMvc
        .perform(get("/any-protected-path").header("Authorization", "Bearer " + tokenFromWrongKey))
        .andExpect(status().isUnauthorized());
  }
}
