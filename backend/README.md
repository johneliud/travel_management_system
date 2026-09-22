# Travel Management System - Backend API

## Prerequisites

- Java 21+
- Maven 3.9+ (or use the included Maven Wrapper `./mvnw`)
- PostgreSQL 15+ (for local development)
- Docker (required for integration tests via Testcontainers)

## Profiles

| Profile         | Database                    | Use Case                          |
|-----------------|-----------------------------|-----------------------------------|
| `local`         | PostgreSQL (local)          | Local development                 |
| `test`          | H2 (in-memory)              | Unit tests                        |
| `integration`   | PostgreSQL (Testcontainers) | Integration tests                 |
| `docker`        | PostgreSQL (container)      | Container-based deployment        |

**Important:** A profile must be explicitly specified. The application will fail fast with a clear error if no profile is active.

## Running Locally

1. Ensure PostgreSQL is running with a `travelmanagementsystem` database
2. Start the application with the `local` profile:

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```

The application starts on port **8080** by default.

### Health Check

```bash
curl http://localhost:8080/actuator/health
# Expected: {"status":"UP"}
```

### API Documentation

Once running, the OpenAPI spec and Swagger UI are available at:

| URL | Description |
|-----|-------------|
| http://localhost:8080/swagger-ui.html | Swagger UI (interactive docs) |
| http://localhost:8080/v3/api-docs | OpenAPI 3.1 spec (JSON) |

The spec auto-generates from controllers and DTOs as they are added.

## Testing

### Run All Tests

```bash
# Unit tests (fast, H2 in-memory)
./mvnw test

# Integration tests (Testcontainers spins up Postgres)
./mvnw clean test -Dspring.profiles.active=integration
```

### Test Profiles

| Profile         | Database                         | Use Case                       |
|-----------------|----------------------------------|--------------------------------|
| `test`          | H2 (in-memory)                   | Unit tests -- fast, no I/O     |
| `integration`   | PostgreSQL (Testcontainers)      | Integration tests -- real DB   |

### Unit Tests

- **Location:** `src/test/java/com/travelmanagementsystem/...`
- **Profile:** `test` (H2 in-memory, Flyway disabled)
- **Convention:** Use `@ActiveProfiles("test")`. Mock external dependencies with Mockito.
- **Naming:** `<ClassName>Test.java`

```java
@SpringBootTest
@ActiveProfiles("test")
class MyServiceTest {
    @Test
    void shouldDoSomething() { ... }
}
```

### Integration Tests

- **Location:** `src/test/java/com/travelmanagementsystem/...`
- **Profile:** `integration` (Testcontainers PostgreSQL, Flyway migrations applied)
- **Convention:** Extend `IntegrationTest` base class. No manual container setup needed.
- **Naming:** `<ClassName>IntegrationTest.java`

```java
@AutoConfigureMockMvc
class MyEndpointIntegrationTest extends IntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldReturn200() throws Exception { ... }
}
```

The `IntegrationTest` base class:
- Starts a shared PostgreSQL 18 container (reused across tests in the same JVM)
- Overrides datasource properties via `@DynamicPropertySource`
- Enables Flyway and validates Hibernate schema

### Architecture Tests

ArchUnit rules enforce module boundary constraints. These run as part of the standard test suite:

- Domain layer must not depend on infrastructure
- Application layer must not depend on infrastructure
- Module API layers must not depend on domain or infrastructure
- Shared package must not depend on any module

### Test Inventory

| Test Class | Tests | Coverage |
|------------|-------|----------|
| `AuthControllerIntegrationTest` | 16 | Registration, login, validation |
| `SecurityIntegrationTest` | 8 | JWT auth, rate limiting, public routes |
| `RefreshLogoutIntegrationTest` | 11 | Token refresh, rotation, logout |
| `RoleAuthorizationIntegrationTest` | 4 | Role-based access (ADMIN, TRAVELER) |
| `UserProfileIntegrationTest` | 11 | Profile get/update, password change |
| `EmailVerificationIntegrationTest` | 6 | OTP verify, expired, reused |
| `AdminUserManagementIntegrationTest` | 17 | CRUD, pagination, suspend, role change |
| `FullAuthLifecycleIntegrationTest` | 1 | End-to-end auth lifecycle |

## Environment Variables

The `docker` profile reads connection details from environment variables or property overrides:

| Variable                 | Description              | Default                                       |
|--------------------------|--------------------------|-----------------------------------------------|
| `SPRING_DATASOURCE_URL`  | JDBC connection URL      | `jdbc:postgresql://localhost:5432/travelmanagementsystem` |
| `SPRING_DATASOURCE_USERNAME` | Database username    | `postgres`                                    |
| `SPRING_DATASOURCE_PASSWORD` | Database password    | `postgres`                                    |
| `JWT_SECRET`             | HMAC signing key (min 32 bytes) | (must be set in production)          |

## Actuator Endpoints

| Profile   | Exposed Endpoints               |
|-----------|----------------------------------|
| `local`   | health, info, env, beans         |
| `test`    | health                           |
| `docker`  | health, info                     |

## Building

```bash
./mvnw clean package
```

The JAR will be in `target/travel_management_system-0.0.1-SNAPSHOT.jar`.

## Module Structure

```
src/main/java/com/travelmanagementsystem/
├── identity/              # Authentication, registration, user management
│   ├── api/               # Controllers, DTOs
│   ├── application/       # Services
│   ├── domain/            # Entities (User, Role, RefreshToken)
│   └── infrastructure/    # Security filters, config, repositories
├── shared/                # Cross-cutting concerns
│   ├── api/               # GlobalExceptionHandler, ErrorResponse
│   ├── config/            # RateLimitFilter, CorsConfig
│   ├── exception/         # Base exceptions
│   └── security/          # Roles constants, JwtPrincipal
└── travel/                # Travel features (Phase 3+)
```
