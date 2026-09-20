# Data Architecture

## Overview

PostgreSQL is the transactional source of truth for the application. All business data is stored in PostgreSQL and managed via Flyway migrations.

## Database

- **Engine**: PostgreSQL 15+
- **Connection**: HikariCP connection pool (Spring Boot defaults)
- **Profiles**: `local` and `docker` connect to PostgreSQL; `test` uses H2 in-memory

## Migration Tool

Flyway manages all database schema changes. Migrations run automatically on application startup.

### Naming Convention

```
V{version}__description.sql
```

- `V` prefix for versioned migrations
- `{version}` is an incrementing integer (1, 2, 3...)
- `__` (double underscore) separates version from description
- Description uses underscores for spaces

Examples:
```
V1__baseline.sql
V2__create_users_table.sql
V3__add_email_index.sql
```

### Migration Ownership

Each module owns its migrations. Place them in:
```
src/main/resources/db/migration/
```

Business tables are created by their owning module, not in the baseline migration.

### Migration Rules

1. **Never modify a committed migration** - create a new one instead
2. **Migrations must be idempotent** - safe to run multiple times
3. **No destructive operations** without a new migration
4. **Use `IF NOT EXISTS`** for CREATE TABLE/Index statements
5. **Use `IF EXISTS`** for DROP statements

## Connection Configuration

Credentials are sourced from environment variables:
- `DATABASE_URL` - JDBC connection URL
- `DATABASE_USERNAME` - Database username
- `DATABASE_PASSWORD` - Database password

See `.env.example` for placeholder values.

## Testing

Integration tests use Testcontainers to spin up a fresh PostgreSQL instance. Tests are independent of the shared development database.

## Identity Module Tables

Owned by the `identity` module (migration `V2__identity_schema.sql`).

### users

| Column | Type | Nullable | Default | Notes |
|--------|------|----------|---------|-------|
| id | BIGSERIAL | no | auto | Primary key |
| email | VARCHAR(255) | no | — | Unique, indexed |
| password_hash | VARCHAR(255) | no | — | Never exposed outside identity |
| status | VARCHAR(20) | no | 'ACTIVE' | CHECK: ACTIVE, INACTIVE, LOCKED |
| email_verified | BOOLEAN | no | false | |
| created_at | TIMESTAMPTZ | no | NOW() | |
| updated_at | TIMESTAMPTZ | no | NOW() | |

### roles

| Column | Type | Nullable | Default | Notes |
|--------|------|----------|---------|-------|
| id | BIGSERIAL | no | auto | Primary key |
| name | VARCHAR(50) | no | — | Unique (ADMIN, TRAVEL_MANAGER, TRAVELER) |
| description | VARCHAR(255) | yes | — | |
| created_at | TIMESTAMPTZ | no | NOW() | |

### user_roles (join table)

| Column | Type | Nullable | Notes |
|--------|------|----------|-------|
| user_id | BIGINT | no | FK → users.id (CASCADE delete) |
| role_id | BIGINT | no | FK → roles.id (CASCADE delete) |
| assigned_at | TIMESTAMPTZ | no | Default NOW() |

Composite primary key: `(user_id, role_id)`
