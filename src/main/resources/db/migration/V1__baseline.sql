-- Baseline migration: create the application schema.
-- All application tables live in the 'app' schema to separate them from
-- extensions and pg_catalog. This also makes privilege grants simpler in
-- production (GRANT USAGE ON SCHEMA app TO secureapi_role).
CREATE SCHEMA IF NOT EXISTS app;
