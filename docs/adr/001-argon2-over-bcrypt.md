# ADR 001 — Argon2id over bcrypt for password hashing

**Status:** Accepted  
**Date:** 2025-05-11

## Context

We need a password hashing algorithm that is resistant to GPU/ASIC-based
brute-force attacks and aligns with current security guidance.

## Decision

Use **Argon2id** via `spring-security-crypto`'s `Argon2PasswordEncoder` with
Spring Security v5.8 default parameters:

| Parameter | Value | Why |
|---|---|---|
| Memory | 64 MiB | High memory cost defeats GPU parallelism; each GPU core needs its own copy |
| Iterations | 2 | Time cost; combined with memory this targets ~100 ms on modern hardware |
| Parallelism | 1 | Single-threaded by default; increase if CPU-bound on multi-core servers |
| Salt length | 16 bytes | 128-bit random salt, per OWASP minimum |
| Hash length | 32 bytes | 256-bit output |

## Why not bcrypt?

| Concern | bcrypt | Argon2id |
|---|---|---|
| GPU resistance | Moderate (memory-hard by design, but 4 KiB only) | Strong (64 MiB by default) |
| OWASP recommendation | Second choice | First choice (since 2019) |
| Parameter agility | Work factor only | Memory + time + parallelism |
| Standard | Old (1999) | Modern (PHC winner 2015, RFC 9106 2021) |

bcrypt's 4 KiB memory footprint is trivially parallelisable on modern GPUs
(thousands of cores, each doing 4 KiB independently). Argon2id's 64 MiB
requirement means a GPU with 8 GB VRAM can only run ~128 parallel attacks —
a 10,000× improvement over bcrypt at equivalent cost.

## Why not scrypt or PBKDF2?

- **scrypt**: Argon2id is preferred by OWASP and RFC 9106 for new applications.
  scrypt's parallel cost parameter is less flexible than Argon2id.
- **PBKDF2**: No memory hardness. OWASP explicitly lists it as third choice,
  acceptable only for FIPS compliance requirements.

## Consequences

- Requires `org.bouncycastle:bcpkix-jdk18on` on the classpath (declared in
  `build.gradle.kts`). Spring Security marks it as an optional compile dep.
- Hash strings include the algorithm and parameters in the prefix
  (`$argon2id$v=19$m=65536,t=2,p=1$...`), enabling transparent parameter
  migration: increase parameters in the future, and existing hashes remain
  valid until users log in and their hash is silently upgraded.
- Hash computation takes ~100 ms per login attempt. This is acceptable UX and
  is the intended defence against brute-force. The login endpoint is also
  rate-limited (Commit 14).
