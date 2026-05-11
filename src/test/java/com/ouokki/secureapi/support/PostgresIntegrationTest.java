package com.ouokki.secureapi.support;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Meta-annotation for integration tests that require a real Postgres instance.
 *
 * <p>Tests annotated with this must declare a static {@code PostgreSQLContainer} field annotated
 * with {@code @Container} and register its properties via {@code @DynamicPropertySource}.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@SpringBootTest
@ActiveProfiles("test")
public @interface PostgresIntegrationTest {}
