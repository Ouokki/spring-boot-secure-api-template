package com.ouokki.secureapi.security;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties(prefix = "app.cors")
@Validated
public record CorsProperties(
    @NotEmpty List<String> allowedOrigins,
    List<String> allowedMethods,
    List<String> allowedHeaders,
    boolean allowCredentials,
    long maxAgeSeconds) {}
