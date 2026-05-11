package com.ouokki.secureapi.security;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties(prefix = "app.jwt")
@Validated
public record JwtProperties(
    @NotBlank String privateKeyPath,
    @NotBlank String publicKeyPath,
    @Min(60) long accessTokenTtlSeconds) {}
