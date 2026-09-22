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

---

## Identity Module Tables

Owned by the `identity` module (migration `V2__identity_schema.sql`).

### users

| Column | Type | Nullable | Default | Notes |
|--------|------|----------|---------|-------|
| id | BIGSERIAL | no | auto | Primary key |
| email | VARCHAR(255) | no | — | Unique, indexed |
| password_hash | VARCHAR(255) | no | — | Never exposed outside identity |
| status | VARCHAR(20) | no | 'ACTIVE' | CHECK: ACTIVE, SUSPENDED, INACTIVE, LOCKED |
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
| user_id | BIGINT | no | FK -> users.id (CASCADE delete) |
| role_id | BIGINT | no | FK -> roles.id (CASCADE delete) |
| assigned_at | TIMESTAMPTZ | no | Default NOW() |

Composite primary key: `(user_id, role_id)`

### refresh_tokens

| Column | Type | Nullable | Default | Notes |
|--------|------|----------|---------|-------|
| id | BIGSERIAL | no | auto | Primary key |
| user_id | BIGINT | no | — | FK -> users.id (CASCADE delete), indexed |
| token_hash | VARCHAR(255) | no | — | SHA-256 hash, indexed |
| expires_at | TIMESTAMPTZ | no | — | |
| revoked_at | TIMESTAMPTZ | yes | — | Null while active |
| created_at | TIMESTAMPTZ | no | NOW() | |

### email_verification_tokens

| Column | Type | Nullable | Default | Notes |
|--------|------|----------|---------|-------|
| id | BIGSERIAL | no | auto | Primary key |
| user_id | BIGINT | no | — | FK -> users.id (CASCADE delete), indexed |
| token_hash | VARCHAR(255) | no | — | SHA-256 hash, indexed |
| expires_at | TIMESTAMPTZ | no | — | |
| used | BOOLEAN | no | false | Single-use |
| created_at | TIMESTAMPTZ | no | NOW() | |

---

## Travel Module Tables

Owned by the `travel` module (migration `V6__travel_schema.sql`).

### travels

| Column | Type | Nullable | Default | Notes |
|--------|------|----------|---------|-------|
| id | BIGSERIAL | no | auto | Primary key |
| title | VARCHAR(255) | no | — | |
| description | TEXT | yes | — | |
| destination_country | VARCHAR(100) | no | — | Indexed |
| destination_city | VARCHAR(100) | no | — | |
| start_date | DATE | no | — | Indexed (composite with end_date) |
| end_date | DATE | no | — | CHECK: >= start_date |
| duration_days | INTEGER | no | — | CHECK: > 0 |
| price | NUMERIC(12,2) | no | — | CHECK: >= 0 |
| capacity | INTEGER | no | — | |
| available_slots | INTEGER | no | — | CHECK: >= 0 AND <= capacity |
| status | VARCHAR(20) | no | 'DRAFT' | CHECK: DRAFT, PUBLISHED, CANCELLED, COMPLETED |
| manager_id | BIGINT | no | — | Logical reference to identity.users (no FK constraint) |
| version | BIGINT | no | 0 | Optimistic locking (V7 migration) |
| created_at | TIMESTAMPTZ | no | NOW() | |
| updated_at | TIMESTAMPTZ | no | NOW() | |

**Cross-module reference note:** `manager_id` references `users.id` in the identity module but is NOT enforced by a foreign key constraint at the database level. Validation happens at the application layer. This keeps module ownership clean -- the travel module owns its tables without depending on identity module table ownership.

### travel_activities

| Column | Type | Nullable | Notes |
|--------|------|----------|-------|
| id | BIGSERIAL | no | Primary key |
| travel_id | BIGINT | no | FK -> travels.id (CASCADE delete), indexed |
| name | VARCHAR(255) | no | |
| description | TEXT | yes | |
| day_number | INTEGER | yes | |
| start_time | TIME | yes | |
| end_time | TIME | yes | |

### travel_transport

| Column | Type | Nullable | Notes |
|--------|------|----------|-------|
| id | BIGSERIAL | no | Primary key |
| travel_id | BIGINT | no | FK -> travels.id (CASCADE delete), indexed |
| type | VARCHAR(50) | no | e.g., FLIGHT, BUS, TRAIN |
| provider | VARCHAR(255) | yes | |
| departure | VARCHAR(255) | yes | |
| arrival | VARCHAR(255) | yes | |
| departure_time | TIMESTAMPTZ | yes | |
| arrival_time | TIMESTAMPTZ | yes | |

---

## Module Ownership Summary

| Module | Tables | Migrations |
|--------|--------|------------|
| identity | users, roles, user_roles, refresh_tokens, email_verification_tokens | V1-V5 |
| travel | travels, travel_activities, travel_transport | V6, V7 |

---

## Concurrency Control

### Slot Reservation

The `reserveSlot()` and `releaseSlot()` methods use **pessimistic row locking** (`SELECT ... FOR UPDATE`) to prevent double-booking:

1. `TravelRepository.findByIdForUpdate(id)` acquires an exclusive row lock on the travel
2. The application checks `available_slots > 0` (or `< capacity` for release)
3. The slot count is updated and saved
4. The transaction commits, releasing the lock

This approach:
- Prevents two concurrent requests from both succeeding on the last slot
- Is simpler to reason about than optimistic locking with retries
- Uses PostgreSQL's `FOR UPDATE` lock, which blocks other transactions until the current one completes
- The `@Version` column provides optimistic locking for other write operations (title changes, status transitions) where the probability of contention is lower

### Optimistic Locking

The `version` column (added in V7) provides optimistic locking for non-slot operations. Hibernate's `@Version` annotation automatically checks the version on `save()` and throws `OptimisticLockingFailureException` if a concurrent modification is detected.
