# Travel Management System - Backend API

## Prerequisites

- Java 21+
- Maven 3.9+ (or use the included Maven Wrapper `./mvnw`)
- PostgreSQL 15+ (for local development)

## Profiles

| Profile         | Database              | Use Case                          |
|-----------------|-----------------------|-----------------------------------|
| `local`         | PostgreSQL            | Local development                 |
| `test`          | H2 (in-memory)        | Unit tests                        |
| `integration`   | PostgreSQL (TC)       | Integration tests                 |
| `docker`        | PostgreSQL            | Container-based deployment        |

**Important:** A profile must be explicitly specified. The application will fail fast with a clear error if no profile is active.

## Running Locally

1. Ensure PostgreSQL is running with a `travelmanagementsystem` database
2. Start the application with the `local` profile:

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```

The application will start on port 8080 by default.

### Health Check

```bash
curl http://localhost:8080/actuator/health
# Expected: {"status":"UP"}
```

## Testing

### Running All Tests

```bash
./mvnw test
```

### Test Profiles

| Profile         | Database              | Use Case                          |
|-----------------|-----------------------|-----------------------------------|
| `test`          | H2 (in-memory)        | Unit tests — fast, no I/O         |
| `integration`   | PostgreSQL (Testcontainers) | Integration tests — real DB   |

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
- **Naming:** `<ClassName>IT.java` or `<ClassName>IntegrationTest.java`

```java
class MyRepositoryIT extends IntegrationTest {

    @Autowired
    private MyRepository repository;

    @Test
    void shouldPersistEntity() { ... }
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

## Environment Variables

The `docker` profile reads connection details from environment variables or property overrides:

| Variable                 | Description              | Default                                       |
|--------------------------|--------------------------|-----------------------------------------------|
| `SPRING_DATASOURCE_URL`  | JDBC connection URL      | `jdbc:postgresql://localhost:5432/travelmanagementsystem` |
| `SPRING_DATASOURCE_USERNAME` | Database username    | `postgres`                                    |
| `SPRING_DATASOURCE_PASSWORD` | Database password    | `postgres`                                    |

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
