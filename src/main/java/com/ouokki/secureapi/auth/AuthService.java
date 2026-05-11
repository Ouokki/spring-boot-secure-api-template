package com.ouokki.secureapi.auth;

import com.ouokki.secureapi.auth.dto.AuthResponse;
import com.ouokki.secureapi.auth.dto.LoginRequest;
import com.ouokki.secureapi.auth.dto.RegisterRequest;
import com.ouokki.secureapi.security.JwtIssuer;
import com.ouokki.secureapi.user.User;
import com.ouokki.secureapi.user.UserRepository;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

  private static final Logger log = LoggerFactory.getLogger(AuthService.class);

  private final UserRepository userRepository;
  private final PasswordHasher passwordHasher;
  private final JwtIssuer jwtIssuer;
  private final RefreshTokenService refreshTokenService;

  public AuthService(
      UserRepository userRepository,
      PasswordHasher passwordHasher,
      JwtIssuer jwtIssuer,
      RefreshTokenService refreshTokenService) {
    this.userRepository = userRepository;
    this.passwordHasher = passwordHasher;
    this.jwtIssuer = jwtIssuer;
    this.refreshTokenService = refreshTokenService;
  }

  @Transactional
  public void register(RegisterRequest request) {
    if (userRepository.existsByEmail(request.email())) {
      log.debug(
          "Registration attempted for already-registered email (suppressed for enumeration prevention)");
      return;
    }
    User user = User.create(request.email(), passwordHasher.hash(request.password()));
    userRepository.save(user);
    log.info("User registered: {}", user.getId());
  }

  @Transactional
  public AuthResponse login(LoginRequest request) {
    User user =
        userRepository
            .findByEmail(request.email())
            .filter(u -> passwordHasher.matches(request.password(), u.getPasswordHash()))
            .orElseThrow(InvalidCredentialsException::new);

    if (!user.isEnabled()) {
      throw new InvalidCredentialsException();
    }

    String accessToken = jwtIssuer.issue(user.getId().toString(), List.of("ROLE_USER"));
    String refreshToken = refreshTokenService.issue(user);
    return new AuthResponse(accessToken, refreshToken);
  }
}
