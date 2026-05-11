package com.ouokki.secureapi.auth;

import com.ouokki.secureapi.auth.dto.AuthResponse;
import com.ouokki.secureapi.auth.dto.LoginRequest;
import com.ouokki.secureapi.auth.dto.RefreshRequest;
import com.ouokki.secureapi.auth.dto.RegisterRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
public class AuthController {

  private final AuthService authService;
  private final RefreshTokenService refreshTokenService;

  public AuthController(AuthService authService, RefreshTokenService refreshTokenService) {
    this.authService = authService;
    this.refreshTokenService = refreshTokenService;
  }

  @PostMapping("/register")
  @ResponseStatus(HttpStatus.CREATED)
  public void register(@Valid @RequestBody RegisterRequest request) {
    authService.register(request);
  }

  @PostMapping("/login")
  public AuthResponse login(@Valid @RequestBody LoginRequest request) {
    return authService.login(request);
  }

  @PostMapping("/refresh")
  public AuthResponse refresh(@Valid @RequestBody RefreshRequest request) {
    return refreshTokenService.rotate(request.refreshToken());
  }
}
