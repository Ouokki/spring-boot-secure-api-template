# spring-boot-secure-api-template

> Production-grade Spring Boot 3 starter — authentication, observability, and the operational concerns tutorials skip.

[![Build](https://github.com/Ouokki/spring-boot-secure-api-template/actions/workflows/ci.yml/badge.svg)](https://github.com/Ouokki/spring-boot-secure-api-template/actions/workflows/ci.yml)

## Tech stack

| Concern | Choice |
|---|---|
| Runtime | Java 21, Spring Boot 3.4.1 |
| Build | Gradle 8.11.1 (Kotlin DSL) |
| Database | PostgreSQL 16 + Flyway + JPA/Hibernate |
| Auth | JWT (RS256) + Argon2id + refresh token rotation |
| Rate limiting | Bucket4j (in-memory, Redis-ready) |
| Observability | Micrometer → Prometheus, Logback JSON, AOP audit log |
| API docs | springdoc-openapi (Swagger UI) |
| Testing | JUnit 5, Testcontainers, ArchUnit |

## Quick start

```bash
# Prerequisites: Java 21, Docker
docker compose up -d          # starts PostgreSQL
./gradlew bootRun             # runs on http://localhost:8080
```

Swagger UI: <http://localhost:8080/swagger-ui.html>

## Running tests

```bash
./gradlew test                # unit + integration (Testcontainers)
./gradlew jacocoTestReport    # coverage report → build/reports/jacoco/
```

---

*More detail added commit-by-commit as features land.*
