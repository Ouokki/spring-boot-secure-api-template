package com.ouokki.secureapi.auth.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.regex.Pattern;

public class PasswordComplexityValidator implements ConstraintValidator<ValidPassword, String> {

  private static final int MIN_LENGTH = 12;
  private static final int MAX_LENGTH = 128;

  // Individual patterns are more readable and produce better error messages than
  // a single mega-regex that nobody can parse in a code review.
  private static final Pattern HAS_UPPERCASE = Pattern.compile("[A-Z]");
  private static final Pattern HAS_LOWERCASE = Pattern.compile("[a-z]");
  private static final Pattern HAS_DIGIT = Pattern.compile("\\d");
  private static final Pattern HAS_SPECIAL = Pattern.compile("[^A-Za-z0-9]");

  @Override
  public boolean isValid(String value, ConstraintValidatorContext ctx) {
    if (value == null) return false;

    int len = value.length();
    if (len < MIN_LENGTH || len > MAX_LENGTH) return false;

    return HAS_UPPERCASE.matcher(value).find()
        && HAS_LOWERCASE.matcher(value).find()
        && HAS_DIGIT.matcher(value).find()
        && HAS_SPECIAL.matcher(value).find();
  }
}
