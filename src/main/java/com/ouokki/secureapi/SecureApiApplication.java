package com.ouokki.secureapi;

import com.ouokki.secureapi.security.JwtProperties;
import com.ouokki.secureapi.security.SecurityProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties({JwtProperties.class, SecurityProperties.class})
public class SecureApiApplication {

  public static void main(String[] args) {
    SpringApplication.run(SecureApiApplication.class, args);
  }
}
