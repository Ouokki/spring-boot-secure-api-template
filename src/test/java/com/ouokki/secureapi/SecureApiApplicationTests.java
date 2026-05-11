package com.ouokki.secureapi;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class SecureApiApplicationTests {

    @Test
    void contextLoads() {
        // Verifies the application context assembles without errors.
        // Database-dependent integration tests live in the integration test suite
        // and require Testcontainers (added in commit 6 onwards).
    }
}
