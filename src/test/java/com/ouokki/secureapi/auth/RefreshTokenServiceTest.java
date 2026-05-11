package com.ouokki.secureapi.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ouokki.secureapi.security.JwtIssuer;
import com.ouokki.secureapi.user.User;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import static org.mockito.Mockito.times;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTest {

  @Mock RefreshTokenRepository refreshTokenRepository;
  @Mock JwtIssuer jwtIssuer;
  RefreshTokenService service;

  @BeforeEach
  void setUp() {
    service = new RefreshTokenService(refreshTokenRepository, jwtIssuer, 30L);
  }

  @Test
  void sha256HexIsConsistentAndNonReversible() {
    String hash1 = RefreshTokenService.sha256Hex("token");
    String hash2 = RefreshTokenService.sha256Hex("token");
    assertThat(hash1).isEqualTo(hash2);
    assertThat(hash1).doesNotContain("token");
    assertThat(hash1).hasSize(64); // SHA-256 hex = 64 chars
  }

  @Test
  void differentInputsProduceDifferentHashes() {
    assertThat(RefreshTokenService.sha256Hex("a")).isNotEqualTo(RefreshTokenService.sha256Hex("b"));
  }

  @Test
  void rotateThrowsWhenTokenNotFound() {
    when(refreshTokenRepository.findByTokenHash(any())).thenReturn(Optional.empty());
    assertThatThrownBy(() -> service.rotate("no-such-token"))
        .isInstanceOf(InvalidCredentialsException.class);
  }

  @Test
  void rotateRevokesEntireFamilyOnRevokedTokenReuse() {
    User user = User.create("test@example.com", "hash");
    UUID familyId = UUID.randomUUID();

    RefreshToken revokedToken =
        RefreshToken.create(
            user,
            RefreshTokenService.sha256Hex("old-token"),
            familyId,
            Instant.now().plusSeconds(3600));
    revokedToken.revoke();

    when(refreshTokenRepository.findByTokenHash(RefreshTokenService.sha256Hex("old-token")))
        .thenReturn(Optional.of(revokedToken));

    assertThatThrownBy(() -> service.rotate("old-token"))
        .isInstanceOf(InvalidCredentialsException.class);

    // Entire family must be revoked — this is the token theft response.
    verify(refreshTokenRepository).revokeFamily(familyId);
  }

  @Test
  void rotateThrowsOnExpiredToken() {
    User user = User.create("test@example.com", "hash");
    RefreshToken expired =
        RefreshToken.create(
            user,
            RefreshTokenService.sha256Hex("expired-token"),
            UUID.randomUUID(),
            Instant.now().minusSeconds(1)); // already expired

    when(refreshTokenRepository.findByTokenHash(RefreshTokenService.sha256Hex("expired-token")))
        .thenReturn(Optional.of(expired));

    assertThatThrownBy(() -> service.rotate("expired-token"))
        .isInstanceOf(InvalidCredentialsException.class);

    // Family revocation must NOT happen for a simple expiry.
    verify(refreshTokenRepository, never()).revokeFamily(any());
  }

  @Test
  void rotateIssuesNewAccessAndRefreshToken() {
    User user = User.create("test@example.com", "hash");
    UUID familyId = UUID.randomUUID();
    RefreshToken valid =
        RefreshToken.create(
            user,
            RefreshTokenService.sha256Hex("valid-token"),
            familyId,
            Instant.now().plusSeconds(3600));

    when(refreshTokenRepository.findByTokenHash(any())).thenReturn(Optional.empty());
    when(refreshTokenRepository.findByTokenHash(RefreshTokenService.sha256Hex("valid-token")))
        .thenReturn(Optional.of(valid));
    when(refreshTokenRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    when(jwtIssuer.issue(any(), any(List.class))).thenReturn("access-token");

    var response = service.rotate("valid-token");

    assertThat(response.accessToken()).isEqualTo("access-token");
    assertThat(response.refreshToken()).isNotBlank();
    assertThat(response.refreshToken()).isNotEqualTo("valid-token");

    // Old token must be revoked — save is called twice: once for the new token, once for the old.
    ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
    verify(refreshTokenRepository, times(2)).save(captor.capture());
    assertThat(captor.getAllValues()).anyMatch(RefreshToken::isRevoked);
  }
}
