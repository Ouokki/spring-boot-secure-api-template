package com.ouokki.secureapi.ratelimit;

import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties(prefix = "app.rate-limit")
@Validated
public record RateLimitProperties(
    @Min(1) int requestsPerMinute, @Min(1) int burstCapacity) {}
