package com.ouokki.secureapi.auth;

import static org.assertj.core.api.Assertions.assertThat;

import com.ouokki.secureapi.auth.validation.PasswordComplexityValidator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class PasswordComplexityValidatorTest {

  private final PasswordComplexityValidator validator = new PasswordComplexityValidator();

  @Test
  void validPasswordPasses() {
    assertThat(validator.isValid("Abcdef1!ghijkl", null)).isTrue();
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "Short1!",
        "alllowercase1!aaaa",
        "ALLUPPERCASE1!AAAA",
        "NoSpecialChar1aaaa",
        "NoDigit!Abcdefghij"
      })
  void invalidPasswordsFail(String password) {
    assertThat(validator.isValid(password, null)).isFalse();
  }

  @Test
  void nullFails() {
    assertThat(validator.isValid(null, null)).isFalse();
  }

  @Test
  void passwordExactlyAtMinLengthPasses() {
    // 12 chars: upper + lower + digit + special
    assertThat(validator.isValid("Abcdefg1!hij", null)).isTrue();
  }

  @Test
  void passwordOver128CharsFails() {
    String tooLong = "Aa1!".repeat(33); // 132 chars
    assertThat(validator.isValid(tooLong, null)).isFalse();
  }
}
