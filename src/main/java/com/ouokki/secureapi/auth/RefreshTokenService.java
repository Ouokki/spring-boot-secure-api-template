package com.ouokki.secureapi.auth;

import com.ouokki.secureapi.auth.dto.AuthResponse;
import com.ouokki.secureapi.security.JwtIssuer;
import com.ouokki.secureapi.user.User;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RefreshTokenService {

  private static final Logger log = LoggerFactory.getLogger(RefreshTokenService.class);
  private static final SecureRandom SECURE_RANDOM = new SecureRandom();

  private final RefreshTokenRepository refreshTokenRepository;
  private final JwtIssuer jwtIssuer;
  private final long refreshTokenTtlDays;

  public RefreshTokenService(
      RefreshTokenRepository refreshTokenRepository,
      JwtIssuer jwtIssuer,
      @Value("${app.jwt.refresh-token-ttl-days:30}") long refreshTokenTtlDays) {
    this.refreshTokenRepository = refreshTokenRepository;
    this.jwtIssuer = jwtIssuer;
    this.refreshTokenTtlDays = refreshTokenTtlDays;
  }

  /** Issues a brand-new refresh token for first login, starting a new token family. */
  @Transactional
  public String issue(User user) {
    return createToken(user, UUID.randomUUID());
  }

  /**
   * Rotates a refresh token: validates the old token, revokes it, issues a new one in the same
   * family.
   *
   * <p>If the presented token was already revoked, the <em>entire family</em> is revoked (suspected
   * theft scenario — someone replayed a token that should have been consumed). See ADR 003 for the
   * threat model and the race condition this handles.
   */
  @Transactional
  public AuthResponse rotate(String rawToken) {
    String hash = sha256Hex(rawToken);

    RefreshToken stored =
        refreshTokenRepository.findByTokenHash(hash).orElseThrow(InvalidCredentialsException::new);

    if (stored.isRevoked()) {
      // Token was already consumed — either a replay attack or a race condition.
      // Revoking the whole family protects the legitimate holder.
      log.warn(
          "Revoked refresh token reuse detected for family {}, revoking entire family",
          stored.getFamilyId());
      refreshTokenRepository.revokeFamily(stored.getFamilyId());
      throw new InvalidCredentialsException();
    }

    if (stored.isExpired()) {
      stored.revoke();
      refreshTokenRepository.save(stored);
      throw new InvalidCredentialsException();
    }

    // Rotate: revoke old, issue new in the same family.
    User user = stored.getUser();
    String newRawToken = createToken(user, stored.getFamilyId());
    String newHash = sha256Hex(newRawToken);
    UUID newId =
        refreshTokenRepository.findByTokenHash(newHash).map(RefreshToken::getId).orElse(null);

    stored.revoke();
    if (newId != null) {
      stored.markReplacedBy(newId);
    }
    refreshTokenRepository.save(stored);

    String accessToken = jwtIssuer.issue(user.getId().toString(), List.of("ROLE_USER"));
    return new AuthResponse(accessToken, newRawToken);
  }

  private String createToken(User user, UUID familyId) {
    byte[] bytes = new byte[32];
    SECURE_RANDOM.nextBytes(bytes);
    String rawToken = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);

    Instant expiresAt = Instant.now().plus(refreshTokenTtlDays, ChronoUnit.DAYS);
    RefreshToken token = RefreshToken.create(user, sha256Hex(rawToken), familyId, expiresAt);
    refreshTokenRepository.save(token);
    return rawToken;
  }

  /**
   * SHA-256 hex digest for storing token hashes at rest.
   *
   * <p>MessageDigest is not thread-safe; a new instance per call is intentional.
   */
  static String sha256Hex(String input) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      return HexFormat.of().formatHex(digest.digest(input.getBytes()));
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException("SHA-256 not available", e);
    }
  }
}
