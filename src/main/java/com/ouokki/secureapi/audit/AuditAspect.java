package com.ouokki.secureapi.audit;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class AuditAspect {

  private static final Logger audit = LoggerFactory.getLogger("AUDIT");

  @Around("@annotation(audited)")
  public Object log(ProceedingJoinPoint pjp, Audited audited) throws Throwable {
    String action = audited.action();
    String principal = resolvePrincipal();

    try {
      Object result = pjp.proceed();
      audit.info("action={} outcome=SUCCESS principal={}", action, principal);
      return result;
    } catch (Throwable t) {
      audit.warn("action={} outcome=FAILURE principal={} error={}", action, principal, t.getMessage());
      throw t;
    }
  }

  private String resolvePrincipal() {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
      return "anonymous";
    }
    return auth.getName();
  }
}
