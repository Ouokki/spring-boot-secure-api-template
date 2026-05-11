package com.ouokki.secureapi.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ouokki.secureapi.auth.AuthService;
import com.ouokki.secureapi.auth.InvalidCredentialsException;
import com.ouokki.secureapi.auth.RefreshTokenRepository;
import com.ouokki.secureapi.auth.RefreshTokenService;
import com.ouokki.secureapi.ratelimit.RateLimitProperties;
import com.ouokki.secureapi.security.JwtIssuer;
import com.ouokki.secureapi.security.SecurityConfig;
import com.ouokki.secureapi.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest
@Import(SecurityConfig.class)
@EnableConfigurationProperties(RateLimitProperties.class)
class GlobalExceptionHandlerTest {

  @Autowired MockMvc mockMvc;

  @MockitoBean JwtIssuer jwtIssuer;
  @MockitoBean AuthService authService;
  @MockitoBean RefreshTokenService refreshTokenService;
  @MockitoBean UserRepository userRepository;
  @MockitoBean RefreshTokenRepository refreshTokenRepository;

  @Test
  void invalidCredentialsReturns401WithProblemDetail() throws Exception {
    Mockito.when(authService.login(Mockito.any())).thenThrow(new InvalidCredentialsException());

    mockMvc
        .perform(
            post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"x@x.com\",\"password\":\"wrong\"}"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.title").value("Invalid credentials"))
        .andExpect(jsonPath("$.status").value(401));
  }

  @Test
  void validationErrorReturns422WithFieldErrors() throws Exception {
    mockMvc
        .perform(
            post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"\",\"password\":\"\"}"))
        .andExpect(status().isUnprocessableEntity())
        .andExpect(jsonPath("$.title").value("Validation failed"))
        .andExpect(jsonPath("$.errors").isArray());
  }
}
