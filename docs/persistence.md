# Persistence

## Migration naming convention

All Flyway migration scripts live in `src/main/resources/db/migration/` and
must follow this pattern:

```
V{n}__{snake_case_description}.sql
```

| Segment | Rule |
|---|---|
| `V{n}` | Monotonically increasing integer, no gaps. Next available is always the highest existing + 1. |
| `__` | Exactly two underscores (Flyway requirement). |
| `snake_case_description` | Lower-case words separated by underscores. Describe the change, not the ticket. |

### Examples

```
V1__baseline.sql
V2__create_users_table.sql
V3__create_refresh_tokens_table.sql
V4__create_audit_log_table.sql
V5__add_users_last_login_at.sql
```

### Rules

- **Never edit a committed migration.** Flyway checksums each script; editing a
  deployed migration will halt startup with a checksum mismatch.
- **Repeatable migrations** (`R__`) are reserved for views and stored functions
  that are safe to re-run. Use sparingly.
- **All DDL goes in migrations.** Never set `spring.jpa.hibernate.ddl-auto` to
  anything other than `validate` in any profile.
- **Test with Testcontainers.** The `test` profile auto-applies all migrations
  against a real Postgres container on every test run. There is no H2 fallback.

## Schema layout

```
public schema   — Postgres extensions (uuid-ossp if needed, pgcrypto, etc.)
app schema      — All application tables
```

Application user `secureapi` must have `USAGE` on schema `app` and standard
`SELECT/INSERT/UPDATE/DELETE` on its tables. DDL (`CREATE TABLE`, `ALTER TABLE`)
should be executed by a separate migration user in production.

## HikariCP pool sizing

Pool size formula (from
[HikariCP docs](https://github.com/brettwooldridge/HikariCP/wiki/About-Pool-Sizing)):

```
pool_size = cpu_count * 2 + effective_spindle_count
```

For a 2-vCPU cloud instance with SSD storage (spindle count = 1):

```
pool_size = 2 * 2 + 1 = 5
```

This is the production default in `application.yml`. Tune per actual hardware.
Over-sizing hurts: each idle Postgres connection consumes ~8 MB of shared memory.
