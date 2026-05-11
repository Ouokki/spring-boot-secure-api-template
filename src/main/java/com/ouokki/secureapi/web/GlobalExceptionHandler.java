package com.ouokki.secureapi.web;

import com.ouokki.secureapi.auth.InvalidCredentialsException;
import java.net.URI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

  private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

  private static final URI INVALID_CREDENTIALS_TYPE =
      URI.create("https://api.example.com/errors/invalid-credentials");
  private static final URI VALIDATION_ERROR_TYPE =
      URI.create("https://api.example.com/errors/validation-error");
  private static final URI INTERNAL_ERROR_TYPE =
      URI.create("https://api.example.com/errors/internal-error");

  @ExceptionHandler(InvalidCredentialsException.class)
  ProblemDetail handleInvalidCredentials(InvalidCredentialsException ex) {
    ProblemDetail detail = ProblemDetail.forStatus(HttpStatus.UNAUTHORIZED);
    detail.setType(INVALID_CREDENTIALS_TYPE);
    detail.setTitle("Invalid credentials");
    detail.setDetail("The supplied credentials are invalid or the account is locked.");
    return detail;
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  ProblemDetail handleValidation(MethodArgumentNotValidException ex) {
    ProblemDetail detail = ProblemDetail.forStatus(HttpStatus.UNPROCESSABLE_ENTITY);
    detail.setType(VALIDATION_ERROR_TYPE);
    detail.setTitle("Validation failed");
    detail.setDetail("One or more request fields failed validation.");
    detail.setProperty(
        "errors",
        ex.getBindingResult().getFieldErrors().stream()
            .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
            .toList());
    return detail;
  }

  @ExceptionHandler(Exception.class)
  ProblemDetail handleUnexpected(Exception ex) {
    // Spring MVC exceptions (404, 405, 415, …) carry their own HTTP status and
    // ProblemDetail body — delegate rather than masking them as 500.
    if (ex instanceof ErrorResponse errorResponse) {
      return errorResponse.getBody();
    }
    log.error("Unhandled exception", ex);
    ProblemDetail detail = ProblemDetail.forStatus(HttpStatus.INTERNAL_SERVER_ERROR);
    detail.setType(INTERNAL_ERROR_TYPE);
    detail.setTitle("Internal server error");
    detail.setDetail("An unexpected error occurred. Please try again later.");
    return detail;
  }
}
