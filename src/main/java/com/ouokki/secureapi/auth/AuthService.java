package com.ouokki.secureapi.auth;

import com.ouokki.secureapi.audit.Audited;
import com.ouokki.secureapi.auth.dto.AuthResponse;
import com.ouokki.secureapi.auth.dto.LoginRequest;
import com.ouokki.secureapi.auth.dto.RegisterRequest;
import com.ouokki.secureapi.security.JwtIssuer;
import com.ouokki.secureapi.security.SecurityProperties;
import com.ouokki.secureapi.user.User;
import com.ouokki.secureapi.user.UserRepository;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
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
  private final SecurityProperties securityProperties;

  // Pre-computed dummy hash used to normalise timing when a user is not found,
  // preventing email enumeration via response-time side-channels.
  private final String dummyHash;

  public AuthService(
      UserRepository userRepository,
      PasswordHasher passwordHasher,
      JwtIssuer jwtIssuer,
      RefreshTokenService refreshTokenService,
      SecurityProperties securityProperties) {
    this.userRepository = userRepository;
    this.passwordHasher = passwordHasher;
    this.jwtIssuer = jwtIssuer;
    this.refreshTokenService = refreshTokenService;
    this.securityProperties = securityProperties;
    this.dummyHash = passwordHasher.hash("__timing_normalization_dummy__");
  }

  @Audited(action = "USER_REGISTER")
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

  @Audited(action = "USER_LOGIN")
  @Transactional
  public AuthResponse login(LoginRequest request) {
    Optional<User> maybeUser = userRepository.findByEmail(request.email());

    if (maybeUser.isEmpty()) {
      // Dummy hash keeps response time comparable to a real verify, preventing
      // timing-based email enumeration.
      passwordHasher.matches(request.password(), dummyHash);
      throw new InvalidCredentialsException();
    }

    User user = maybeUser.get();

    if (user.isLocked()) {
      log.debug("Login rejected — account locked: {}", user.getId());
      throw new InvalidCredentialsException();
    }

    if (!user.isEnabled()) {
      throw new InvalidCredentialsException();
    }

    if (!passwordHasher.matches(request.password(), user.getPasswordHash())) {
      user.recordFailedLogin(
          securityProperties.maxLoginAttempts(),
          Duration.ofMinutes(securityProperties.lockoutDurationMinutes()));
      userRepository.save(user);
      if (user.isLocked()) {
        log.warn("Account locked after repeated failures: {}", user.getId());
      }
      throw new InvalidCredentialsException();
    }

    user.resetFailedLogins();
    userRepository.save(user);

    String accessToken = jwtIssuer.issue(user.getId().toString(), List.of("ROLE_USER"));
    String refreshToken = refreshTokenService.issue(user);
    return new AuthResponse(accessToken, refreshToken);
  }
}
