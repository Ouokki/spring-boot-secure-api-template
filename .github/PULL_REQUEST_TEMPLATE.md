## Summary

<!-- One paragraph: what changed and why. Link the relevant issue if applicable. -->

Closes #

## Type of change

- [ ] Bug fix (non-breaking)
- [ ] New feature (non-breaking)
- [ ] Breaking change
- [ ] Refactor / chore
- [ ] Documentation

## Checklist

- [ ] Tests added / updated and all pass (`./gradlew test`)
- [ ] Coverage does not drop below thresholds (`./gradlew jacocoTestCoverageVerification`)
- [ ] Spotless and Checkstyle pass (`./gradlew spotlessCheck checkstyleMain`)
- [ ] OWASP scan shows no new high/critical CVEs (`./gradlew dependencyCheckAnalyze`)
- [ ] `application.yml` changes are backward-compatible or documented
- [ ] No secrets, credentials, or real RSA keys committed
- [ ] ADR added/updated if this introduces a new architectural decision

## Security considerations

<!-- If this PR touches auth, crypto, rate-limiting, CORS, or token handling,
     describe the threat model impact here. Otherwise delete this section. -->
