package com.ouokki.secureapi.audit;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a method for audit logging. The aspect records the action name, outcome (SUCCESS/FAILURE),
 * and the authenticated principal (if any) at INFO level.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Audited {

  /** Human-readable action name written to the audit log. */
  String action();
}
