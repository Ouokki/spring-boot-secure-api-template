package com.ouokki.secureapi.auth;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class PasswordHasherTest {

  private final PasswordHasher hasher = new PasswordHasher();

  @Test
  void correctPasswordMatches() {
    String hash = hasher.hash("correct-horse-battery-staple");
    assertThat(hasher.matches("correct-horse-battery-staple", hash)).isTrue();
  }

  @Test
  void wrongPasswordDoesNotMatch() {
    String hash = hasher.hash("correct-horse-battery-staple");
    assertThat(hasher.matches("wrong-password", hash)).isFalse();
  }

  @Test
  void hashIsNeverStoredAsPlaintext() {
    String raw = "my-secret-password";
    String hash = hasher.hash(raw);
    assertThat(hash).doesNotContain(raw);
    // Argon2 encoded hashes start with $argon2id$
    assertThat(hash).startsWith("$argon2id$");
  }

  @Test
  void twoHashesOfSamePasswordDiffer() {
    // Salts are random; identical input must not produce identical output.
    String h1 = hasher.hash("password");
    String h2 = hasher.hash("password");
    assertThat(h1).isNotEqualTo(h2);
  }

  @Test
  void currentParametersDoNotNeedUpgrade() {
    // Hashes produced by the current encoder should not be flagged for re-hashing.
    String hash = hasher.hash("password");
    assertThat(hasher.needsUpgrade(hash)).isFalse();
  }
}
