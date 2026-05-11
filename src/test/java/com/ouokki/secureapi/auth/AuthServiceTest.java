package com.ouokki.secureapi.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ouokki.secureapi.auth.dto.LoginRequest;
import com.ouokki.secureapi.security.JwtIssuer;
import com.ouokki.secureapi.security.SecurityProperties;
import com.ouokki.secureapi.user.User;
import com.ouokki.secureapi.user.UserRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

  @Mock UserRepository userRepository;
  @Mock PasswordHasher passwordHasher;
  @Mock JwtIssuer jwtIssuer;
  @Mock RefreshTokenService refreshTokenService;

  AuthService service;

  @BeforeEach
  void setUp() {
    when(passwordHasher.hash(anyString())).thenReturn("dummy-hash");
    service =
        new AuthService(
            userRepository,
            passwordHasher,
            jwtIssuer,
            refreshTokenService,
            new SecurityProperties(5, 15));
  }

  @Test
  void loginThrowsForUnknownEmail() {
    when(userRepository.findByEmail(any())).thenReturn(Optional.empty());
    when(passwordHasher.matches(any(), any())).thenReturn(false);

    assertThatThrownBy(() -> service.login(new LoginRequest("x@x.com", "pw")))
        .isInstanceOf(InvalidCredentialsException.class);

    // Timing-normalisation hash must run even when user is absent.
    verify(passwordHasher).matches(any(), anyString());
  }

  @Test
  void loginThrowsForWrongPassword() {
    User user = User.create("a@b.com", "hash");
    when(userRepository.findByEmail(any())).thenReturn(Optional.of(user));
    when(passwordHasher.matches(any(), any())).thenReturn(false);

    assertThatThrownBy(() -> service.login(new LoginRequest("a@b.com", "wrong")))
        .isInstanceOf(InvalidCredentialsException.class);

    assertThat(user.getFailedLoginAttempts()).isEqualTo(1);
  }

  @Test
  void loginLocksAccountAfterMaxFailedAttempts() {
    User user = User.create("a@b.com", "hash");
    when(userRepository.findByEmail(any())).thenReturn(Optional.of(user));
    when(passwordHasher.matches(any(), any())).thenReturn(false);

    for (int i = 0; i < 5; i++) {
      assertThatThrownBy(() -> service.login(new LoginRequest("a@b.com", "wrong")))
          .isInstanceOf(InvalidCredentialsException.class);
    }

    assertThat(user.isLocked()).isTrue();
  }

  @Test
  void loginThrowsForLockedAccount() {
    User user = User.create("a@b.com", "hash");
    // Force locked state by exhausting attempts once.
    when(userRepository.findByEmail(any())).thenReturn(Optional.of(user));
    when(passwordHasher.matches(any(), any())).thenReturn(false);
    for (int i = 0; i < 5; i++) {
      try {
        service.login(new LoginRequest("a@b.com", "wrong"));
      } catch (InvalidCredentialsException ignored) {
      }
    }

    // Even with a correct password the locked check fires first — no password verify occurs.
    assertThatThrownBy(() -> service.login(new LoginRequest("a@b.com", "correct")))
        .isInstanceOf(InvalidCredentialsException.class);

    verify(refreshTokenService, never()).issue(any());
  }

  @Test
  void loginResetsFailedAttemptsOnSuccess() {
    User user = User.create("a@b.com", "hash");
    when(userRepository.findByEmail(any())).thenReturn(Optional.of(user));
    when(passwordHasher.matches(any(), any())).thenReturn(false);

    // One failed attempt.
    assertThatThrownBy(() -> service.login(new LoginRequest("a@b.com", "wrong")))
        .isInstanceOf(InvalidCredentialsException.class);
    assertThat(user.getFailedLoginAttempts()).isEqualTo(1);

    // Successful login resets the counter.
    when(passwordHasher.matches(any(), any())).thenReturn(true);
    when(jwtIssuer.issue(any(), any())).thenReturn("access");
    when(refreshTokenService.issue(any())).thenReturn("refresh");
    service.login(new LoginRequest("a@b.com", "correct"));

    assertThat(user.getFailedLoginAttempts()).isEqualTo(0);
    assertThat(user.getLockedUntil()).isNull();
  }
}
