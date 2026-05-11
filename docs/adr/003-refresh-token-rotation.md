# ADR 003 — Refresh token rotation with family revocation

**Status:** Accepted  
**Date:** 2025-05-11

## Context

Access tokens have a short TTL (15 min) to limit blast radius. Users need a
mechanism to obtain new access tokens without re-entering credentials. Refresh
tokens fill that role, but they are long-lived (30 days) and therefore a higher
value theft target.

## Decision

Implement **refresh token rotation with family revocation**:

1. Each refresh token is **single-use**. On rotation, the old token is revoked
   and a new token issued in the same *family*.
2. If a **revoked** token is presented (replay attack), the **entire family is
   revoked**. This forces the legitimate user to re-authenticate.
3. Tokens are stored as **SHA-256 hashes** — the raw token is never persisted.
4. Token comparison uses `MessageDigest` (via `sha256Hex`) — not `String.equals`
   — which avoids timing side-channels.

## Token family concept

A *family* is a UUID shared by all tokens issued in a single login session.
When a user logs in, a new family is created. Each rotation produces a new
token in the same family. The chain looks like:

```
Login → T1 (family: F)
         ↓ rotate
        T2 (family: F, replaces T1)
         ↓ rotate
        T3 (family: F, replaces T2)
```

If T2 is presented after it has been rotated (meaning T3 was already issued),
T2 is revoked. This means either:
- The legitimate holder's T3 was stolen, or
- The attacker stole T2 before the legitimate holder could rotate it.

Either way, revoking the entire family F forces re-authentication and protects
the account. The legitimate user loses their session; the attacker loses theirs.

## The race condition

**Scenario:** Token T2 is rotated and T3 issued. The network request carrying
T3 back to the client is lost. The client retries with T2 (it was not
discarded). T2 is now revoked, triggering family revocation.

**Mitigation:** This is a known trade-off — a brief re-login is preferable to
a persistent session with a stolen token. Client implementations should handle
401 on refresh by redirecting to login. A grace window (accepting both old and
new token for a few seconds) could mitigate this at the cost of complexity;
it is not implemented here.

## Storage

```sql
refresh_tokens (
    id          UUID PRIMARY KEY,
    user_id     UUID NOT NULL,
    token_hash  TEXT UNIQUE NOT NULL,   -- SHA-256 hex of raw token
    family_id   UUID NOT NULL,
    replaced_by UUID,                   -- audit chain
    revoked_at  TIMESTAMPTZ,
    expires_at  TIMESTAMPTZ NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL
)
```

The `replaced_by` column creates a linked list for forensic audit of a
session's rotation history.

## Consequences

- Re-login required after token theft is detected.
- Re-login required in the race condition described above.
- Old tokens accumulate in the DB; a background job should purge tokens where
  `expires_at < NOW() - INTERVAL '7 days'` to keep the table compact.
