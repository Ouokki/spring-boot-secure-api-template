# Contributing

Contributions — bug reports, feature suggestions, and pull requests — are welcome.

## Development workflow

1. Fork the repository and create a feature branch from `develop`:
   ```bash
   git checkout -b feat/my-feature develop
   ```
2. Generate dev RSA keys and start PostgreSQL (see README Quick start).
3. Make your changes. Run the full test suite before committing:
   ```bash
   ./gradlew spotlessCheck checkstyleMain test
   ```
4. Commit using the [Conventional Commits](https://www.conventionalcommits.org/) format:
   ```
   feat(auth): add email verification flow
   fix(ratelimit): reset bucket on 429 response
   docs: update configuration reference
   ```
5. Open a pull request against `develop`. The CI workflow must pass.

## Code style

- **Formatting**: Spotless enforces Google Java Format. Auto-fix with `./gradlew spotlessApply`.
- **Checkstyle**: rules in `config/checkstyle/checkstyle.xml`. Fix before pushing.
- **No commented-out code**, **no `TODO` committed**, **no magic numbers** without a named constant.

## Testing expectations

- All new business logic must have unit tests.
- Services that interact with the database must have a Testcontainers integration test.
- Architecture rules in `ArchitectureTest` must continue to pass.
- Target ≥ 80 % line coverage (enforced by JaCoCo from commit 21 onward).

## Submitting a security issue

Please **do not open a public issue** for security vulnerabilities.
Email [ouokkimohamed7@gmail.com](mailto:ouokkimohamed7@gmail.com) with the subject
`[SECURITY] spring-boot-secure-api-template`. You will receive a response within 72 hours.
