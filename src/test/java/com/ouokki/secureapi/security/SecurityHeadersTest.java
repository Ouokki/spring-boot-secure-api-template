package com.ouokki.secureapi.security;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ouokki.secureapi.auth.AuthService;
import com.ouokki.secureapi.auth.RefreshTokenRepository;
import com.ouokki.secureapi.auth.RefreshTokenService;
import com.ouokki.secureapi.user.UserRepository;
import com.ouokki.secureapi.ratelimit.RateLimitProperties;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest
@Import(SecurityConfig.class)
@EnableConfigurationProperties(RateLimitProperties.class)
class SecurityHeadersTest {

  @Autowired MockMvc mockMvc;

  @MockitoBean JwtIssuer jwtIssuer;
  @MockitoBean AuthService authService;
  @MockitoBean RefreshTokenService refreshTokenService;
  @MockitoBean UserRepository userRepository;
  @MockitoBean RefreshTokenRepository refreshTokenRepository;

  @Test
  void everyResponseCarriesOwaspSecurityHeaders() throws Exception {
    mockMvc
        .perform(get("/api/protected").secure(true))
        .andExpect(status().isUnauthorized()) // JWT auth required → 401
        .andExpect(header().string("X-Content-Type-Options", "nosniff"))
        .andExpect(header().string("X-Frame-Options", "DENY"))
        .andExpect(header().exists("Strict-Transport-Security"))
        .andExpect(header().exists("Content-Security-Policy"))
        .andExpect(header().exists("Referrer-Policy"))
        .andExpect(header().exists("Permissions-Policy"));
  }
}
