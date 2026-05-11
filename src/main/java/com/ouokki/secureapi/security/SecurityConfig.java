package com.ouokki.secureapi.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import java.time.Instant;
import java.util.Map;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

  private final JwtAuthenticationFilter jwtAuthenticationFilter;
  private final ObjectMapper objectMapper;

  public SecurityConfig(
      JwtAuthenticationFilter jwtAuthenticationFilter, ObjectMapper objectMapper) {
    this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    this.objectMapper = objectMapper;
  }

  @Bean
  public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    http.csrf(csrf -> csrf.disable())
        .headers(
            headers ->
                headers
                    // HSTS: browsers remember HTTPS-only for 1 year, including sub-domains.
                    .httpStrictTransportSecurity(
                        hsts ->
                            hsts.maxAgeInSeconds(31_536_000)
                                .includeSubDomains(true)
                                .preload(true))
                    // Prevent the response being embedded in a frame (clickjacking).
                    .frameOptions(frame -> frame.deny())
                    // Prevent MIME-type sniffing.
                    .contentTypeOptions(Customizer.withDefaults())
                    // Disable legacy XSS auditor — modern recommendation per OWASP.
                    .xssProtection(xss -> xss.disable())
                    // CSP: pure API — no scripts, styles, or frames needed.
                    .contentSecurityPolicy(
                        csp -> csp.policyDirectives("default-src 'none'; frame-ancestors 'none'"))
                    // Referrer-Policy: omit referrer for cross-origin requests.
                    .referrerPolicy(
                        referrer ->
                            referrer.policy(
                                ReferrerPolicyHeaderWriter.ReferrerPolicy
                                    .STRICT_ORIGIN_WHEN_CROSS_ORIGIN))
                    // Permissions-Policy: disable browser features the API will never use.
                    .permissionsPolicy(
                        perm ->
                            perm.policy("camera=(), microphone=(), geolocation=(), payment=()")))
        .sessionManagement(
            session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
        .exceptionHandling(
            ex ->
                ex.authenticationEntryPoint(
                    (request, response, authException) -> {
                      // Returns a minimal JSON 401 body. The full RFC 7807 wrapper is added
                      // in commit 18 when the global exception handler is wired.
                      response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                      response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                      objectMapper.writeValue(
                          response.getWriter(),
                          Map.of(
                              "status",
                              401,
                              "error",
                              "Unauthorized",
                              "timestamp",
                              Instant.now().toString()));
                    }))
        .authorizeHttpRequests(
            auth ->
                auth.requestMatchers(
                        "/auth/**",
                        "/actuator/health",
                        "/actuator/info",
                        // OpenAPI spec and Swagger UI — disable in prod via springdoc.* properties.
                        "/v3/api-docs/**",
                        "/swagger-ui/**",
                        "/swagger-ui.html")
                    .permitAll()
                    .anyRequest()
                    .authenticated());

    return http.build();
  }
}
