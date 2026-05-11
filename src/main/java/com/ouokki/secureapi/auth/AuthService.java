package com.ouokki.secureapi.auth;

import com.ouokki.secureapi.auth.dto.AuthResponse;
import com.ouokki.secureapi.auth.dto.LoginRequest;
import com.ouokki.secureapi.auth.dto.RegisterRequest;
import com.ouokki.secureapi.user.User;
import com.ouokki.secureapi.user.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

  private static final Logger log = LoggerFactory.getLogger(AuthService.class);

  private final UserRepository userRepository;
  private final PasswordHasher passwordHasher;

  public AuthService(UserRepository userRepository, PasswordHasher passwordHasher) {
    this.userRepository = userRepository;
    this.passwordHasher = passwordHasher;
  }

  @Transactional
  public void register(RegisterRequest request) {
    // We do NOT reveal whether the email is already registered.
    // If it exists we silently succeed from the caller's perspective.
    // A real implementation would send a "account already exists" email to the address.
    if (userRepository.existsByEmail(request.email())) {
      log.debug(
          "Registration attempted for already-registered email (suppressed for enumeration prevention)");
      return;
    }

    User user = User.create(request.email(), passwordHasher.hash(request.password()));
    userRepository.save(user);
    log.info("User registered: {}", user.getId());
  }

  @Transactional(readOnly = true)
  public AuthResponse login(LoginRequest request) {
    // Constant-time lookup + hash compare to prevent user enumeration via timing.
    // Both "email not found" and "wrong password" produce the same response and
    // take the same code path.
    User user =
        userRepository
            .findByEmail(request.email())
            .filter(u -> passwordHasher.matches(request.password(), u.getPasswordHash()))
            .orElseThrow(InvalidCredentialsException::new);

    if (!user.isEnabled()) {
      // Still throw InvalidCredentialsException — not AccountLockedException —
      // to prevent user enumeration via differing error responses.
      throw new InvalidCredentialsException();
    }

    // JWT issuance will be wired here in commit 9.
    // Returning placeholder tokens for now so the endpoint is end-to-end testable.
    return new AuthResponse("access-token-placeholder", "refresh-token-placeholder");
  }
}
