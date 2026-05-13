plugins {
    java
    id("org.springframework.boot") version "3.4.1"
    id("io.spring.dependency-management") version "1.1.7"
    id("com.diffplug.spotless") version "6.25.0"
    id("info.solidsoft.pitest") version "1.15.0"
    checkstyle
    jacoco
}

group = "com.ouokki"
version = "0.0.1-SNAPSHOT"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

repositories {
    mavenCentral()
}

val jjwtVersion = "0.12.6"
val testcontainersVersion = "1.20.4"
val archunitVersion = "1.3.0"
val bucket4jVersion = "8.10.1"
val logstashEncoderVersion = "8.0"
val springdocVersion = "2.7.0"
val bouncycastleVersion = "1.78.1"

dependencies {
    // Core web + security
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-aop")

    // Persistence
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.flywaydb:flyway-core")
    implementation("org.flywaydb:flyway-database-postgresql")
    runtimeOnly("org.postgresql:postgresql")

    // JWT — RS256 signing
    implementation("io.jsonwebtoken:jjwt-api:$jjwtVersion")
    runtimeOnly("io.jsonwebtoken:jjwt-impl:$jjwtVersion")
    runtimeOnly("io.jsonwebtoken:jjwt-jackson:$jjwtVersion")

    // Rate limiting
    implementation("com.bucket4j:bucket4j-core:$bucket4jVersion")

    // Metrics / observability
    implementation("io.micrometer:micrometer-registry-prometheus")
    implementation("net.logstash.logback:logstash-logback-encoder:$logstashEncoderVersion")

    // OpenAPI / Swagger UI
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:$springdocVersion")

    // Argon2id — BouncyCastle is an optional dep of spring-security-crypto; must be declared explicitly
    implementation("org.bouncycastle:bcpkix-jdk18on:$bouncycastleVersion")

    // Generates META-INF/spring-configuration-metadata.json for @ConfigurationProperties IDE support
    annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")

    // Testing
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.security:spring-security-test")
    testImplementation("org.testcontainers:junit-jupiter:$testcontainersVersion")
    testImplementation("org.testcontainers:postgresql:$testcontainersVersion")
    testImplementation("com.tngtech.archunit:archunit-junit5:$archunitVersion")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

// ─── Spotless ────────────────────────────────────────────────────────────────
spotless {
    java {
        importOrder()
        removeUnusedImports()
        googleJavaFormat("1.22.0")
        formatAnnotations()
        // The comment below stays out — Spotless strips trailing newlines itself.
        trimTrailingWhitespace()
        endWithNewline()
    }
    kotlinGradle {
        trimTrailingWhitespace()
        endWithNewline()
    }
}

// ─── Checkstyle ──────────────────────────────────────────────────────────────
checkstyle {
    toolVersion = "10.18.2"
    configFile = file("config/checkstyle/checkstyle.xml")
    isIgnoreFailures = false
    maxWarnings = 0
}

// ─── JaCoCo ──────────────────────────────────────────────────────────────────
jacoco {
    toolVersion = "0.8.12"
}

tasks.jacocoTestReport {
    dependsOn(tasks.test)
    reports {
        xml.required = true
        html.required = true
    }
}

tasks.jacocoTestCoverageVerification {
    dependsOn(tasks.jacocoTestReport)
    violationRules {
        rule {
            limit {
                minimum = "0.80".toBigDecimal()
            }
        }
    }
}

tasks.check {
    dependsOn(tasks.jacocoTestCoverageVerification)
}

// ─── PIT Mutation Testing ────────────────────────────────────────────────────
// Run with: ./gradlew pitest
// Reports land in build/reports/pitest/
pitest {
    junit5PluginVersion = "1.2.1"
    // Target only the business-logic packages — skip generated/config classes.
    targetClasses = setOf(
        "com.ouokki.secureapi.auth.*",
        "com.ouokki.secureapi.user.*",
        "com.ouokki.secureapi.ratelimit.*",
        "com.ouokki.secureapi.observability.*",
        "com.ouokki.secureapi.audit.*"
    )
    targetTests = setOf("com.ouokki.secureapi.*")
    // Exclude Spring-generated proxies and config classes from mutation.
    excludedClasses = setOf(
        "com.ouokki.secureapi.*Config",
        "com.ouokki.secureapi.*Properties",
        "com.ouokki.secureapi.SecureApiApplication"
    )
    mutators = setOf("DEFAULTS")
    // Aim for 70 % mutation coverage; raise as the suite matures.
    mutationThreshold = 70
    outputFormats = setOf("HTML", "XML")
    threads = 2
    // Avoid hitting Docker/Testcontainers in mutation runs.
    avoidCallsTo = setOf("org.testcontainers")
}

// ─── Test ────────────────────────────────────────────────────────────────────
tasks.withType<Test> {
    useJUnitPlatform()
    finalizedBy(tasks.jacocoTestReport)
}
