# Security Policy

## Supported Versions

| Version  | Supported |
| -------- | --------- |
| 0.x      | Yes       |

## Reporting a Vulnerability

**Do not open a public GitHub issue for security vulnerabilities.**

Email **ouokkimohamed7@gmail.com** with the subject:

```
[SECURITY] <brief description>
```

Include:
- A description of the vulnerability and its potential impact
- Steps to reproduce or a proof-of-concept
- Any suggested mitigations you are aware of

You will receive an acknowledgement within **48 hours** and a full status update within **7 days**.

## Disclosure Policy

Once a fix is available:
1. A patched release is published
2. A GitHub Security Advisory is opened with CVE details
3. The reporter is credited in the release notes (unless anonymity is preferred)

## Scope

This is a **reference / starter template**. It contains:
- No real user data or production credentials
- Mock RSA keys in `dev-keys/` — **never use these in production**
- No live deployment by default

Security concerns most relevant to forks of this template:
- Dependency vulnerabilities (Renovate opens PRs automatically)
- Misconfigured CORS allow-list in production
- Weak or leaked RSA private keys
- Over-permissive Actuator endpoint exposure

## Security Design Decisions

The template documents its security choices in Architecture Decision Records:

| ADR | Decision |
|-----|----------|
| [ADR 001](docs/adr/001-argon2-over-bcrypt.md) | Argon2id over bcrypt for password hashing |
| [ADR 002](docs/adr/002-rs256-over-hs256.md) | RS256 over HS256 for JWT signing |
| [ADR 003](docs/adr/003-refresh-token-rotation.md) | Family-based refresh token rotation |
