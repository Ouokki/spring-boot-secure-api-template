package com.ouokki.secureapi.observability;

import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.boot.actuate.autoconfigure.metrics.MeterRegistryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

@Configuration
public class MetricsConfig {

  @Bean
  public MeterRegistryCustomizer<MeterRegistry> commonTags(Environment env) {
    String appName = env.getProperty("spring.application.name", "unknown");
    return registry -> registry.config().commonTags("application", appName);
  }
}
