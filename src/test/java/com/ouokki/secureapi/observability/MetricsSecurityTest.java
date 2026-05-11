package com.ouokki.secureapi.observability;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ouokki.secureapi.auth.AuthService;
import com.ouokki.secureapi.auth.RefreshTokenRepository;
import com.ouokki.secureapi.auth.RefreshTokenService;
import com.ouokki.secureapi.ratelimit.RateLimitProperties;
import com.ouokki.secureapi.security.JwtIssuer;
import com.ouokki.secureapi.security.SecurityConfig;
import com.ouokki.secureapi.user.UserRepository;
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
class MetricsSecurityTest {

  @Autowired MockMvc mockMvc;

  @MockitoBean JwtIssuer jwtIssuer;
  @MockitoBean AuthService authService;
  @MockitoBean RefreshTokenService refreshTokenService;
  @MockitoBean UserRepository userRepository;
  @MockitoBean RefreshTokenRepository refreshTokenRepository;

  @Test
  void prometheusEndpointRequiresAuthentication() throws Exception {
    mockMvc.perform(get("/actuator/prometheus")).andExpect(status().isUnauthorized());
  }

  @Test
  void healthEndpointIsNotBlockedBySecurity() throws Exception {
    // Actuator endpoints are not served in the @WebMvcTest slice, so 404 is expected —
    // the key assertion is that security does NOT return 401 for this public path.
    mockMvc
        .perform(get("/actuator/health"))
        .andExpect(
            result ->
                org.assertj.core.api.Assertions.assertThat(result.getResponse().getStatus())
                    .isNotEqualTo(401));
  }
}
