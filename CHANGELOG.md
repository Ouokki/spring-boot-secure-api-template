# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

## [0.1.0] – 2025-05-01

### Added
- JWT authentication with RS256 signing (ADR 002)
- Refresh-token rotation with family-based reuse detection (ADR 003)
- Argon2id password hashing (ADR 001)
- Role-based access control (`ROLE_USER`, `ROLE_ADMIN`)
- Global rate-limiting via Bucket4j (per-IP, per-user buckets)
- CORS configuration with allow-list driven by `app.cors.allowed-origins`
- Spring Security filter chain with stateless sessions
- Actuator endpoints locked to `ROLE_ADMIN` with `/actuator/health` public
- OpenAPI 3 documentation at `/swagger-ui.html` (dev profile only)
- OWASP dependency-check wired into CI
- Spotless + Checkstyle (Google style) code-quality gates
- JaCoCo coverage report at 80 % line threshold
- Docker multi-stage build producing a minimal JRE 21 image
- Dev RSA key pair in `dev-keys/` (never use in production)
- Architecture Decision Records: ADR 001–003

[Unreleased]: https://github.com/Ouokki/spring-boot-secure-api-template/compare/v0.1.0...HEAD
[0.1.0]: https://github.com/Ouokki/spring-boot-secure-api-template/releases/tag/v0.1.0
