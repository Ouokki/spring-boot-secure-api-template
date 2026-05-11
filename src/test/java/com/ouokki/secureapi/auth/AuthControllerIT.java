package com.ouokki.secureapi.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ouokki.secureapi.auth.dto.LoginRequest;
import com.ouokki.secureapi.auth.dto.RegisterRequest;
import com.ouokki.secureapi.support.PostgresIntegrationTest;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.Base64;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@PostgresIntegrationTest
@Testcontainers(disabledWithoutDocker = true)
@AutoConfigureMockMvc
class AuthControllerIT {

  @TempDir static Path keyDir;

  @Container
  static PostgreSQLContainer<?> postgres =
      new PostgreSQLContainer<>("postgres:16-alpine")
          .withDatabaseName("secureapi")
          .withUsername("secureapi")
          .withPassword("test-secret");

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
  }

  @DynamicPropertySource
  static void registerProps(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", postgres::getJdbcUrl);
    registry.add("spring.datasource.username", postgres::getUsername);
    registry.add("spring.datasource.password", postgres::getPassword);
    registry.add("app.jwt.private-key-path", () -> keyDir.resolve("private.pem").toString());
    registry.add("app.jwt.public-key-path", () -> keyDir.resolve("public.pem").toString());
  }

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;

  @Test
  void registerHappyPath() throws Exception {
    mockMvc
        .perform(
            post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(
                        new RegisterRequest("new@example.com", "Str0ng!Password"))))
        .andExpect(status().isCreated());
  }

  @Test
  void registerWithDuplicateEmailReturns201WithoutRevealingExistence() throws Exception {
    var req =
        objectMapper.writeValueAsString(new RegisterRequest("dup@example.com", "Str0ng!Password"));

    mockMvc
        .perform(post("/auth/register").contentType(MediaType.APPLICATION_JSON).content(req))
        .andExpect(status().isCreated());
    mockMvc
        .perform(post("/auth/register").contentType(MediaType.APPLICATION_JSON).content(req))
        .andExpect(status().isCreated());
  }

  @Test
  void registerWithWeakPasswordReturns400() throws Exception {
    mockMvc
        .perform(
            post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(new RegisterRequest("x@example.com", "weak"))))
        .andExpect(status().isBadRequest());
  }

  @Test
  void registerWithInvalidEmailReturns400() throws Exception {
    mockMvc
        .perform(
            post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(
                        new RegisterRequest("not-an-email", "Str0ng!Password"))))
        .andExpect(status().isBadRequest());
  }

  @Test
  void loginHappyPath() throws Exception {
    String email = "login@example.com";
    String password = "Str0ng!Password";
    mockMvc
        .perform(
            post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new RegisterRequest(email, password))))
        .andExpect(status().isCreated());

    var result =
        mockMvc
            .perform(
                post("/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(new LoginRequest(email, password))))
            .andExpect(status().isOk())
            .andReturn();

    String body = result.getResponse().getContentAsString();
    assertThat(body).contains("accessToken");
  }

  @Test
  void loginWithWrongPasswordReturns401() throws Exception {
    mockMvc
        .perform(
            post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(
                        new LoginRequest("nobody@example.com", "Str0ng!Password"))))
        .andExpect(status().isUnauthorized());
  }
}
