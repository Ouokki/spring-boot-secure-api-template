package com.ouokki.secureapi.auth;

import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * Wraps Argon2id hashing with parameters tuned to OWASP recommendations.
 *
 * <p>OWASP (https://cheatsheetseries.owasp.org/cheatsheets/Password_Storage_Cheat_Sheet.html)
 * minimum for Argon2id: memory=19 MiB, iterations=2, parallelism=1. The values below are the Spring
 * Security defaults for v5.8+ which match those minimums. Increase memory as hardware allows; hash
 * time should target ~100 ms on production hardware to frustrate brute force while remaining
 * acceptable for login UX.
 *
 * <p>Parameter migration: if you increase the parameters later, existing hashes remain valid
 * because {@link #matches} delegates to Spring Security which reads the encoded algorithm and
 * parameters from the hash prefix. New logins automatically use the updated parameters.
 */
@Service
public class PasswordHasher {

  // memory=65536 KiB (64 MiB), iterations=2 (time cost), parallelism=1, hash length=32, salt=16
  // These are the Spring Security v5.8 defaults and exceed OWASP minimums.
  private static final Argon2PasswordEncoder ENCODER =
      Argon2PasswordEncoder.defaultsForSpringSecurity_v5_8();

  public String hash(String rawPassword) {
    return ENCODER.encode(rawPassword);
  }

  public boolean matches(String rawPassword, String encodedPassword) {
    return ENCODER.matches(rawPassword, encodedPassword);
  }

  /**
   * Returns true if the stored hash was produced with older parameters and should be re-hashed on
   * next successful login.
   */
  public boolean needsUpgrade(String encodedPassword) {
    return ENCODER.upgradeEncoding(encodedPassword);
  }
}
