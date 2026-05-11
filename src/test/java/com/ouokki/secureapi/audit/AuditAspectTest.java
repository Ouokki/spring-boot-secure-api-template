package com.ouokki.secureapi.audit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {AuditAspectTest.TestConfig.class, AuditAspect.class})
class AuditAspectTest {

  @Autowired AuditTarget target;

  ListAppender<ILoggingEvent> logCapture;

  @BeforeEach
  void attachLogCapture() {
    Logger auditLogger = (Logger) LoggerFactory.getLogger("AUDIT");
    logCapture = new ListAppender<>();
    logCapture.start();
    auditLogger.addAppender(logCapture);
    auditLogger.setLevel(Level.INFO);
  }

  @AfterEach
  void detachLogCapture() {
    Logger auditLogger = (Logger) LoggerFactory.getLogger("AUDIT");
    auditLogger.detachAppender(logCapture);
  }

  @Test
  void successfulCallLogsSuccessOutcome() {
    target.doWork();

    assertThat(logCapture.list)
        .anyMatch(
            e ->
                e.getFormattedMessage().contains("action=DO_WORK")
                    && e.getFormattedMessage().contains("outcome=SUCCESS"));
  }

  @Test
  void failingCallLogsFailureOutcomeAndRethrows() {
    assertThatThrownBy(() -> target.doFail()).isInstanceOf(IllegalStateException.class);

    assertThat(logCapture.list)
        .anyMatch(
            e ->
                e.getFormattedMessage().contains("action=DO_FAIL")
                    && e.getFormattedMessage().contains("outcome=FAILURE"));
  }

  // ─── Minimal test double ──────────────────────────────────────────────────

  static class AuditTarget {
    @Audited(action = "DO_WORK")
    void doWork() {}

    @Audited(action = "DO_FAIL")
    void doFail() {
      throw new IllegalStateException("boom");
    }
  }

  @Configuration
  @EnableAspectJAutoProxy
  static class TestConfig {
    @Bean
    AuditTarget auditTarget() {
      return new AuditTarget();
    }
  }
}
