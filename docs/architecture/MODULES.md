# Module Architecture

## Overview

The backend follows a **package-by-module** structure where each business domain is isolated into its own module. Modules communicate only through public API contracts, never through internal implementation details.

## Module List

| Module           | Responsibility                                      |
|------------------|-----------------------------------------------------|
| `identity`       | User authentication, authorization, profiles        |
| `travel`         | Trip/itinerary management, bookings                 |
| `subscription`   | Subscription plans, billing cycles                  |
| `payment`        | Payment processing, refunds, transaction history    |
| `feedback`       | Reviews, ratings, surveys                           |
| `reporting`      | Analytics dashboards, exported reports              |
| `search`         | Full-text search, filtering, indexing               |
| `recommendation` | Travel suggestions, personalization                 |
| `ai`             | AI-powered features, LLM integration                |
| `analytics`      | Event tracking, metrics aggregation                 |
| `notification`   | Email, push, in-app notifications                   |
| `shared`         | Cross-cutting: base exceptions, common value objects|

## Four-Layer Convention

Every module (except `shared`) follows the same internal structure:

```
<module>/
  api/            # Public contracts: DTOs, request/response objects, facade interfaces
  application/    # Use-case orchestration: service interfaces, command/query handlers
  domain/         # Core business logic: entities, value objects, domain events, repository interfaces
  infrastructure/ # Implementation details: repository impls, external clients, config, mappers
```

### Layer Rules

| Layer            | Contains                                         | Visibility      |
|------------------|--------------------------------------------------|-----------------|
| `api/`           | DTOs, facade interfaces, request/response models | **Public**      |
| `application/`   | Use-case handlers, service interfaces            | **Public**      |
| `domain/`        | Entities, value objects, repository interfaces    | Package-private |
| `infrastructure/`| Repository impls, mappers, external adapters      | Package-private |

### What Belongs Where

- **api/**: Anything the outside world (other modules, controllers, tests) needs to see. DTOs, command objects, facade interfaces.
- **application/**: Orchestration logic. Calls domain services, coordinates transactions. No business rules here.
- **domain/**: Pure business logic. Entities, value objects, domain events, repository interfaces (not implementations). Should have no framework dependencies.
- **infrastructure/**: Technical implementations. JPA repositories, HTTP clients, email senders, message brokers. Implements interfaces defined in domain/.

## Boundary Rules

### Rule 1: No Reaching Into Another Module's Internals

A module may **never** directly import from another module's `domain/` or `infrastructure/` packages.

**Allowed:**
```
identity.application → travel.api (facade interface)
identity.application → shared.exception (base exception)
```

**Forbidden:**
```
identity.application → travel.infrastructure (internal implementation)
identity.domain → travel.domain (direct entity access)
```

### Rule 2: Cross-Module Communication

Modules communicate through:
1. **API interfaces** defined in `<module>/api/` or `<module>/application/`
2. **Domain events** (Phase 7+)
3. **Shared kernel** (`shared/`) for truly cross-cutting concerns

### Rule 3: Domain Layer Isolation

The `domain/` layer must not depend on:
- Any module's `infrastructure/` layer
- Any module's `application/` layer (except its own)
- Framework-specific annotations (except basic Jakarta/Javax)

### Rule 4: Shared Package Restrictions

The `shared/` package must not depend on any module. It is a leaf dependency.

## Enforcement

These rules are **automatically enforced** via ArchUnit tests in `ArchitectureTest.java`. The tests run as part of `mvn test` and will fail the build if any boundary violation is detected.

To run the architecture tests:
```bash
./mvnw test -Dtest=ArchitectureTest
```

## Adding a New Module

1. Create the four-layer package structure:
   ```
   src/main/java/com/travelmanagementsystem/<module>/
     api/
     application/
     domain/
     infrastructure/
   ```

2. Add `package-info.java` to each package.

3. The ArchUnit tests will automatically enforce boundaries for the new module.

## Package Structure

```
com.travelmanagementsystem
  ├── identity/
  │   ├── api/
  │   ├── application/
  │   ├── domain/
  │   └── infrastructure/
  ├── travel/
  │   ├── api/
  │   ├── application/
  │   ├── domain/
  │   └── infrastructure/
  ├── ... (other modules)
  └── shared/
      ├── api/          # GlobalExceptionHandler, ErrorResponse
      ├── config/       # RateLimitFilter, CorsConfig
      ├── exception/    # DomainException, NotFoundException, BusinessException
      ├── security/     # Roles constants (single source of truth for role names)
      └── valueobject/  # Common value objects
```

## Travel Offering Lifecycle

Travel offerings progress through a state machine:

```
DRAFT -> publish -> PUBLISHED -> cancel -> CANCELLED
  │                                           │
  └───────────────────────────────────────────┘
                  (any state -> CANCELLED)
```

### Status Transitions

| From      | To          | Trigger          | Endpoint                           | Constraint                                    |
|-----------|-------------|------------------|------------------------------------|-----------------------------------------------|
| DRAFT     | PUBLISHED   | Manager/Admin    | `POST /api/travels/{id}/publish`   | All required fields must be populated          |
| DRAFT     | CANCELLED   | Manager/Admin    | `POST /api/travels/{id}/cancel`    | —                                              |
| PUBLISHED | CANCELLED   | Manager/Admin    | `POST /api/travels/{id}/cancel`    | Phase 4/5 must handle active subscribers        |
| COMPLETED | —           | System           | (automated)                        | Travel date has passed                         |
| CANCELLED | —           | —                | —                                  | Terminal state; no transitions out              |

### Field Locking After Publication

Once PUBLISHED, the following fields are **locked** to avoid breaking existing subscriber expectations:
- `title`, `destinationCountry`, `destinationCity`
- `startDate`, `endDate`, `durationDays`
- `price`, `capacity`

**Editable after publication:** `description`, `activities`, `transport`

### Ownership Rules

- A **Travel Manager** may only edit/publish/cancel their own travel offerings
- An **Admin** may edit/publish/cancel any travel offering
- A **Traveler** may not create, edit, or publish travel offerings

## Role-Based Access Control

### Roles Constants

All role names are defined as constants in `shared.security.Roles`. Every module references roles by these constants — never by magic strings.

```java
public final class Roles {
    public static final String ADMIN = "ADMIN";
    public static final String TRAVEL_MANAGER = "TRAVEL_MANAGER";
    public static final String TRAVELER = "TRAVELER";
}
```

### Enforcing Role Requirements

Method-level security is enabled via `@EnableMethodSecurity` on the security configuration. Endpoints enforce role requirements using `@PreAuthorize`:

```java
@GetMapping("/users")
@PreAuthorize("hasRole('" + Roles.ADMIN + "')")
public ResponseEntity<List<UserSummaryResponse>> listUsers() { ... }
```

### Role Hierarchy

Roles are checked **explicitly per endpoint** — there is no implicit hierarchy. An `ADMIN` user must be explicitly granted access to admin-only endpoints; having a higher-level role does not implicitly satisfy lower-level checks. This avoids confusing edge cases as more roles are added.

### Adding Role Checks to New Modules

1. Import `com.travelmanagementsystem.shared.security.Roles`
2. Annotate the service or controller method with `@PreAuthorize("hasRole('" + Roles.ROLE_NAME + "')")`
3. The JWT filter already extracts roles from tokens and populates `SimpleGrantedAuthority("ROLE_<name>")` — no additional wiring needed
4. Add an `@ExceptionHandler(AccessDeniedException.class)` in the module's exception handler (or rely on the shared `GlobalExceptionHandler`) to return 403
