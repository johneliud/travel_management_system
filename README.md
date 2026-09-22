# Travel Management System

Full-stack application for managing travel bookings, itineraries, and trip packages.

## Tech Stack

| Layer    | Technology                                      |
|----------|-------------------------------------------------|
| Frontend | Angular 22, Tailwind CSS v4, Vitest             |
| Backend  | Java 21, Spring Boot 4, Spring Security, JPA   |
| Database | PostgreSQL 18 (Testcontainers for tests)        |

## Project Structure

```
.
├── backend/          # Spring Boot REST API
├── frontend/         # Angular SSR application
├── docs/             # Architecture decisions, API conventions
│   ├── api/          # API error/response conventions
│   ├── architecture/ # Module boundaries, data model
│   └── decisions/    # ADRs
├── infrastructure/   # Docker, Terraform (future)
```

## Getting Started

### Prerequisites

- **Java 21+** and **Maven 3.9+** (or use `./mvnw`)
- **Node.js 20+** and **npm 11+**
- **Docker** (required for Testcontainers during integration tests)

### Clone

```bash
git clone https://github.com/johneliud/travel_management_system.git
cd travel_management_system
```

### Backend

```bash
cd backend/

# Start locally (requires PostgreSQL running locally)
./mvnw spring-boot:run -Dspring-boot.run.profiles=local

# Run all tests (Testcontainers spins up Postgres automatically)
./mvnw clean test -Dspring-profiles.active=integration
```

See [`backend/README.md`](backend/README.md) for full details on profiles, testing, and environment variables.

### Frontend

```bash
cd frontend/

npm install
npm start # Dev server at http://localhost:4200
```

See [`frontend/README.md`](frontend/README.md) for scripts, folder structure, and conventions.

## Documentation

| Document | Description |
|----------|-------------|
| [`backend/README.md`](backend/README.md) | Backend setup, profiles, testing, env vars |
| [`frontend/README.md`](frontend/README.md) | Frontend setup, scripts, folder conventions |
| [`docs/api/README.md`](docs/api/README.md) | API error format, status codes, conventions |
| [`docs/architecture/MODULES.md`](docs/architecture/MODULES.md) | Module boundaries, ArchUnit rules |
| [`docs/architecture/DATA.md`](docs/architecture/DATA.md) | Data model and entity relationships |
