package com.ouokki.secureapi.security;

import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
public class CorsConfig {

  private final CorsProperties props;

  public CorsConfig(CorsProperties props) {
    this.props = props;
  }

  @Bean
  public CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration config = new CorsConfiguration();
    config.setAllowedOrigins(props.allowedOrigins());
    config.setAllowedMethods(
        props.allowedMethods() != null
            ? props.allowedMethods()
            : List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
    config.setAllowedHeaders(
        props.allowedHeaders() != null ? props.allowedHeaders() : List.of("*"));
    config.setAllowCredentials(props.allowCredentials());
    config.setMaxAge(props.maxAgeSeconds() > 0 ? props.maxAgeSeconds() : 1800L);

    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", config);
    return source;
  }
}
