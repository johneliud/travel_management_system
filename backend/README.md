# Travel Management System - Backend API

## Prerequisites

- Java 21+
- Maven 3.9+ (or use the included Maven Wrapper `./mvnw`)
- PostgreSQL 15+ (for local development)

## Profiles

| Profile   | Database    | Use Case                          |
|-----------|-------------|-----------------------------------|
| `local`   | PostgreSQL  | Local development                 |
| `test`    | H2 (in-memory) | Unit/integration tests        |
| `docker`  | PostgreSQL  | Container-based deployment        |

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

## Running Tests

Tests use the `test` profile with an embedded H2 database (no external dependencies needed):

```bash
./mvnw test
```

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
