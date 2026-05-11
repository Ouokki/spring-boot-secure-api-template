# spring-boot-secure-api-template

> Production-grade Spring Boot 3.4 REST API — authentication, security hardening,
> observability, and the operational concerns that tutorials skip.

[![CI](https://github.com/Ouokki/spring-boot-secure-api-template/actions/workflows/ci.yml/badge.svg)](https://github.com/Ouokki/spring-boot-secure-api-template/actions/workflows/ci.yml)

## Features

| Area | What's included |
|------|----------------|
| **Auth** | JWT RS256 access tokens (15 min TTL), refresh token rotation with family revocation, Argon2id password hashing |
| **Security** | Account lockout after N failed logins, OWASP HTTP headers (HSTS, CSP, X-Frame-Options, Referrer-Policy, Permissions-Policy) |
| **Rate limiting** | Per-IP token bucket via Bucket4j — `X-RateLimit-*` and `Retry-After` headers |
| **Observability** | Correlation ID propagation (MDC + `X-Correlation-Id`), structured JSON logging (logstash-logback-encoder), Micrometer → Prometheus |
| **Audit** | `@Audited` AOP annotation — structured `action / outcome / principal` records |
| **API** | RFC 7807 Problem Details, OpenAPI 3 / Swagger UI (toggleable per environment) |
| **Database** | PostgreSQL 16, Flyway, HikariCP with tuned pool config |
| **Testing** | 70+ unit tests, Testcontainers integration tests, ArchUnit architecture rules, PIT mutation testing, JaCoCo coverage |
| **Build & ops** | Spotless, Checkstyle, multi-stage Dockerfile, GitHub Actions CI, Renovate auto-updates |

## Architecture

```
HTTP request
  ↓  CorrelationIdFilter   — sets X-Correlation-Id in MDC
  ↓  RateLimitFilter       — Bucket4j token bucket, 429 on breach
  ↓  JwtAuthenticationFilter — RS256 Bearer token verification
  ↓  SecurityFilterChain   — OWASP headers, authz rules
  ↓  DispatcherServlet
       ↓
  AuthController ──→ AuthService  (@Audited, @Transactional)
                          ↓
               UserRepository / RefreshTokenRepository
                          ↓
                    PostgreSQL  (schema managed by Flyway)
```

## Tech stack

| Concern | Choice |
|---------|--------|
| Runtime | Java 21, Spring Boot 3.4.1 |
| Build | Gradle 8.11.1 (Kotlin DSL) |
| Database | PostgreSQL 16 + Flyway + JPA/Hibernate |
| Auth | JWT RS256 (JJWT 0.12) + Argon2id (BouncyCastle) + refresh rotation |
| Rate limiting | Bucket4j 8.10 (in-memory; swap to Redis for multi-node) |
| Observability | Micrometer → Prometheus, Logback JSON (logstash-encoder), AOP audit |
| API docs | springdoc-openapi 2.7 (Swagger UI) |
| Testing | JUnit 5, Mockito, Testcontainers, ArchUnit, PIT |

## Quick start

### Prerequisites

- Java 21
- Docker (for PostgreSQL)
- `openssl` (for dev RSA key generation)

### 1 — Generate RSA dev keys

```bash
mkdir -p dev-keys
openssl genrsa -out dev-keys/private.pem 2048
openssl rsa -in dev-keys/private.pem -pubout -out dev-keys/public.pem
```

> `dev-keys/` is in `.gitignore`. Never commit real keys.

### 2 — Start PostgreSQL

```bash
docker compose up -d
```

### 3 — Run the application

```bash
./gradlew bootRun
```

- API: `http://localhost:8080`
- Swagger UI: `http://localhost:8080/swagger-ui.html`
- Management: `http://localhost:8081/actuator/health`

## API reference

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| `POST` | `/auth/register` | — | Create account |
| `POST` | `/auth/login` | — | Obtain access + refresh tokens |
| `POST` | `/auth/refresh` | — | Rotate refresh token |
| `GET` | `/actuator/health` | — | Liveness / readiness |
| `GET` | `/actuator/prometheus` | JWT | Prometheus metrics scrape |

## Configuration reference

| Variable | Default | Description |
|----------|---------|-------------|
| `JWT_PRIVATE_KEY_PATH` | `dev-keys/private.pem` | RS256 signing key path |
| `JWT_PUBLIC_KEY_PATH` | `dev-keys/public.pem` | RS256 verification key path |
| `JWT_ACCESS_TOKEN_TTL_SECONDS` | `900` | Access token lifetime (seconds) |
| `MAX_LOGIN_ATTEMPTS` | `5` | Consecutive failures before lockout |
| `LOCKOUT_DURATION_MINUTES` | `15` | Account lockout duration |
| `RATE_LIMIT_REQUESTS_PER_MINUTE` | `60` | Requests per client IP per minute |
| `RATE_LIMIT_BURST_CAPACITY` | `10` | Initial burst allowance |
| `MANAGEMENT_PORT` | `8081` | Dedicated actuator port |
| `SPRINGDOC_API_DOCS_ENABLED` | `true` | Set `false` in prod to disable OpenAPI |

## Running tests

```bash
# Unit + integration tests with JaCoCo coverage
./gradlew test jacocoTestReport

# Mutation testing (run before releases — takes several minutes)
./gradlew pitest
```

## Project structure

```
src/main/java/com/ouokki/secureapi/
├── auth/           # Login, register, refresh token rotation
├── audit/          # @Audited AOP annotation + AuditAspect
├── observability/  # CorrelationIdFilter, MetricsConfig
├── ratelimit/      # RateLimitFilter + RateLimitProperties
├── security/       # JwtIssuer, JwtAuthFilter, SecurityConfig
├── user/           # User entity + UserRepository
└── web/            # GlobalExceptionHandler, OpenApiConfig
```

## Design decisions

See [`docs/adr/`](docs/adr/) for Architecture Decision Records:

- [ADR 001](docs/adr/001-argon2id-password-hashing.md) — Why Argon2id over bcrypt
- [ADR 002](docs/adr/002-rs256-jwt-signing.md) — Why RS256 over HS256
- [ADR 003](docs/adr/003-refresh-token-rotation.md) — Refresh token rotation and token-theft response

## License

MIT — see [LICENSE](LICENSE).
