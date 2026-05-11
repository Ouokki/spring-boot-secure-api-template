package com.ouokki.secureapi.security;

import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties(prefix = "app.security")
@Validated
public record SecurityProperties(
    @Min(1) int maxLoginAttempts, @Min(1) int lockoutDurationMinutes) {}
