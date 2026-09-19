# Frontend

Angular 22 application with SSR, Tailwind CSS v4, and Vitest.

## Prerequisites

- Node.js 20+
- npm 11+

## Local Development

```bash
npm install
npm start
```

The app runs at `http://localhost:4200/`.

### Environment Configuration

The API base URL is configured via the `API_BASE_URL` environment variable:

```bash
API_BASE_URL=http://localhost:8080 npm start
```

If not set, it defaults to `http://localhost:8080`. The environment file is auto-generated at build time by `scripts/setup-environment.ts`.

## Scripts

| Command | Description |
|---------|-------------|
| `npm start` | Start dev server |
| `npm run build` | Production build |
| `npm test` | Run unit tests (Vitest) |
| `npm run lint` | Lint TypeScript files |
| `npm run lint:fix` | Lint and auto-fix |
| `npm run format` | Format with Prettier |
| `npm run format:check` | Check formatting |

## Folder Structure

```
src/app/
├── core/                    # Singleton services, guards, interceptors
│   ├── auth/                # Auth-related types and utilities
│   ├── guards/              # Route guards (authGuard, etc.)
│   ├── interceptors/        # HTTP interceptors (apiPrefix, auth)
│   └── services/            # Environment config, API base service
├── shared/                  # Reusable components, directives, pipes
│   ├── components/
│   ├── directives/
│   └── pipes/
├── features/                # Feature modules (lazy-loaded)
│   ├── auth/                # Phase 2: Login, register
│   ├── travel/              # Phase 3: Trips, bookings
│   └── ...                  # Future features
├── app.ts                   # Root component
├── app.html                 # Root template (shell layout)
├── app.config.ts            # Client-side providers
├── app.routes.ts            # Route definitions
└── app.spec.ts              # Root component tests
```

### Conventions

- **`core/`** — Singleton services, guards, and interceptors. Never imported by other core modules.
- **`shared/`** — Reusable UI primitives. Can be imported by any feature module.
- **`features/`** — One directory per feature. Each has its own routes, components, and services. Lazy-loaded via `app.routes.ts`.
- **Environment access** — Use `inject(ENVIRONMENT)` to get the `apiBaseUrl` and other config.

## Tech Stack

- Angular 22 (standalone components, signals, native control flow)
- Tailwind CSS v4
- Vitest (unit testing)
- ESLint + Prettier
- SSR via Express + `@angular/ssr`
