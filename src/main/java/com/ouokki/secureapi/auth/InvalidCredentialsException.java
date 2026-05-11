package com.ouokki.secureapi.auth;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Thrown when authentication fails for ANY reason — wrong password, unknown email, locked account.
 *
 * <p>A single exception type prevents callers (including exception handlers) from inadvertently
 * leaking the specific failure reason to HTTP clients, which would enable user enumeration. The
 * global exception handler (commit 18) will wrap this in RFC 7807 Problem Details.
 */
@ResponseStatus(HttpStatus.UNAUTHORIZED)
public class InvalidCredentialsException extends RuntimeException {

  public InvalidCredentialsException() {
    super("Invalid credentials");
  }
}
