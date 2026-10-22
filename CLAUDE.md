# Scheduler

Multi-tenant medical appointment scheduler. Each **clinic** is a tenant; its staff (administrators, doctors, assistants) manage patients, doctor availability, schedule exceptions and appointments. Appointment changes are pushed live to a board over WebSocket, and patients can book through a WhatsApp chatbot (Twilio + Spring AI / Gemini).

## Repository layout

```
scheduler/
├── backend/        Spring Boot REST API (Gradle)
├── frontend/       Angular SPA (npm)
├── render.yaml     Render deployment (both services as Docker)
└── .claude/        project skills and agents
```

## Backend — `backend/`

- **Stack:** Java 26, Spring Boot 4, Spring Data JPA / Hibernate, PostgreSQL, Flyway, Spring Security + jjwt (JWT), Spring WebSocket (STOMP), springdoc OpenAPI, MapStruct, Lombok, Spring AI (Google GenAI), Twilio, Spring Mail.
- **Package:** `com.example.scheduler` → `config`, `config/tenant`, `controller`, `dto/<feature>`, `entity`, `enums`, `exception`, `mapper`, `middleware`, `repository`, `security`, `service` + `service/impl`, `tool`, `util`, `validator`, `websocket`.
- **Domain:** `Clinic` (public schema) · `Personal` (staff, roles `ADMINISTRATOR | DOCTOR | ASSISTANT`, optional `Specialty`) · `Patient` (many-to-many with doctors via `doctor_patient`) · `DoctorAvailability` · `ScheduleException` · `Appointment` (`PENDING | CONFIRMED | CANCELLED`).
- **Multi-tenancy (schema per clinic):** the `public` schema holds `clinics`; each clinic has its own schema `clinic_<id>`. `TenantContext` (ThreadLocal) is set from the JWT `clinicId` claim in `JwtAuthFilter` / `JwtChannelInterceptor` and always cleared in `finally`; `CustomTenantResolver` falls back to `public`. New tenant schemas are created by `SchemaProvisioningService`. Any code that runs outside a request (seeders, schedulers, async) must set and clear the tenant itself.
- **Migrations:** `src/main/resources/db/migration/public` (public schema) and `.../tenant` (applied to every clinic schema). `ddl-auto: none`; never edit an applied migration, add `V{n}__description.sql`.
- **Auth:** login `POST /api/auth/login` with username `<user>@<clinic-subdomain>`; the token carries `sub` (user id), `role`, `clinicId`, `clinicName`, `clinicSubdomain`, `username`. Every request re-checks the user is active with the same role (`ActiveUserChecker`). Public paths come from `security.public-paths`.
- **WebSocket:** STOMP endpoint `/ws`, topic `/topic/clinic/{clinicId}/board`; events are published with `@TransactionalEventListener(AFTER_COMMIT)`.
- **Errors:** services throw `ApiException` subclasses; `GlobalExceptionHandler` returns the `ErrorResponse` JSON.
- **Config:** `application.yaml` + `application-dev.yaml`; secrets from env vars / git-ignored `.env` (template in `backend/.env.example`). Business time zone via `app.zone` and an injected `Clock`.
- **Dev data:** `config/DataSeeder` seeds two clinics with staff and patients when the DB is empty (credentials are in that class).
- **Commands (from `backend/`):** `./gradlew bootRun` · `./gradlew build` · `./gradlew test` (Windows: `gradlew.bat`). Swagger UI at `http://localhost:8080/swagger-ui/index.html`. Requires PostgreSQL.

## Frontend — `frontend/`

- **Stack:** Angular 19 (standalone components, signals, `rxResource`), Angular CDK (dialogs, overlays), RxJS, Tailwind CSS 4, `@stomp/rx-stomp`, `jwt-decode`, Karma/Jasmine, Prettier (`printWidth: 100`, single quotes).
- **Structure:** `src/app/core` (api clients, auth, http, models, navigation, utils) · `features/<feature>` (page + `services/` + `components/`) · `layout/shell` · `shared` (ui, forms, pipes, services, utils, shared appointment dialogs).
- **API access:** relative URLs (`'patients'`) prefixed by `apiUrlInterceptor` with `environment.apiUrl` (`/api/`); `authInterceptor` adds the bearer token and logs out on 401. Types mirroring backend DTOs live in `core/models/api.models.ts`.
- **Auth & roles:** `AuthService` keeps the session (decoded JWT) in a signal + `localStorage`; routes use `authGuard`, `guestGuard`, `roleGuard(...ROLES.x)`; menu items per role in `core/navigation/navigation.ts`.
- **UI language:** Spanish (texts, `LOCALE_ID = 'es'`). Code, identifiers and comments in English.
- **Dev proxy:** `proxy.conf.js` forwards `/api` and `/ws` to `BACKEND_URL` (default `http://localhost:8080`).
- **Commands (from `frontend/`):** `npm start` (http://localhost:4200) · `npx ng build` · `npx ng test --watch=false`.

## Generated artifacts (git-ignored)

- `qa-reports/` — API QA reports from `java-tester`.
- `playwright-mcp/` — screenshots and UI reports from `ui-tester`.

## Skills and agents

| Area     | Skills                                                                                                                                               | Agents                                                            |
|----------|------------------------------------------------------------------------------------------------------------------------------------------------------|-------------------------------------------------------------------|
| Backend  | `java-mvc-project-structure`, `java-entity-modeling`, `java-jpa-query-strategies`, `java-jwt-security`, `java-websocket-stomp`, `java-common-config` | `java-expert-developer`, `code-reviewer-backend`, `java-tester`   |
| Frontend | `angular-developer`, `angular-project-structure`                                                                                                     | `angular-expert-developer`, `code-reviewer-frontend`, `ui-tester` |

The skills are generic (fictional examples); when they differ from this project's existing code, the existing code wins.

## Orchestration

The main session is the **orchestrator**: it analyzes, delegates, consolidates and decides. Subagents cannot spawn other subagents, so every delegation happens from the main session. Apply this flow to any implementation task; trivial questions or one-line fixes don't need it.

### 1. Analyze the requirement (always first, done by the orchestrator)

- Restate the goal and identify the affected area: backend, frontend or both.
- Read the relevant code (controllers, services, entities, pages, routes) to find what exists and what must change.
- Define **acceptance criteria** and the cases that must work (roles, validations, edge cases).
- Ask the user only for decisions that can't be inferred; otherwise choose sensible defaults and state them.
- For full-stack tasks, run the **backend flow first** (it defines the API contract), then the **frontend flow** with the final contract.

### 2. Backend flow

```
analyze ─▶ java-expert-developer ─▶ ┬─ code-reviewer-backend ─┐
                 ▲                  └─ java-tester ───────────┤ (parallel)
                 │                                            ▼
                 └──────── consolidated issues ◀──── orchestrator evaluates
```

1. **Implement** — delegate to `java-expert-developer` with: the goal, acceptance criteria, affected files/endpoints, and any constraints (tenant handling, migrations, roles).
2. **Verify in parallel** — when the developer finishes, launch **in the same message** (so they run concurrently):
   - `code-reviewer-backend` with the scope of the change (files / diff) and the acceptance criteria.
   - `java-tester` with the endpoints to test and the acceptance criteria.
3. **Consolidate** — wait for both. Merge review findings and failed test cases into one deduplicated list, ordered by severity (Critical → Major → Minor), each with `path:line` and the evidence (report path for tests).
4. **Fix loop** — send the consolidated list back to `java-expert-developer` (continue the same agent with `SendMessage` so it keeps its context; start a new one only if it is no longer available). Then repeat step 2 on the new changes.
5. **Stop** when the issue count is at its minimum:
   - no Critical or Major findings, **and**
   - no FAIL test cases (BLOCKED only for documented environment reasons), **and**
   - remaining Minor items/suggestions are either fixed or consciously accepted.
   Also stop after **3 fix iterations** if issues stop decreasing, and report what remains and why.

### 3. Frontend flow

Same loop with the frontend agents:

```
analyze ─▶ angular-expert-developer ─▶ ┬─ code-reviewer-frontend ─┐
                 ▲                     └─ ui-tester ──────────────┤ (parallel)
                 │                                                ▼
                 └──────── consolidated issues ◀──── orchestrator evaluates
```

1. **Implement** — delegate to `angular-expert-developer` with the goal, acceptance criteria, affected pages/routes and the backend contract (endpoints, DTOs, error cases).
2. **Verify in parallel** — in the same message launch `code-reviewer-frontend` (scope of the change) and `ui-tester` (pages/flows, roles and acceptance criteria). The backend must be running for `ui-tester`.
3. **Consolidate** findings + failed UI cases (with screenshot paths) into one prioritized list.
4. **Fix loop** — send it to `angular-expert-developer` via `SendMessage`, then verify again in parallel.
5. **Stop** with the same criteria as the backend flow (no Critical/Major, no FAIL, max 3 iterations without improvement).

### 4. Keep this file up to date

After the loop ends, the orchestrator checks whether the changes made by the agents invalidate anything in this `CLAUDE.md`, and updates it **only if needed**:

- **Update** when a change affects the documented context: new or removed module/package/feature folder, new domain entity, role or status, changes to multi-tenancy, auth/JWT claims, WebSocket endpoints/topics, migration layout, new dependency or version bump of the main stack, new env vars, commands, ports, generated folders, or new/renamed skills and agents.
- **Don't update** for ordinary work that fits the existing description (a new endpoint, page, DTO, bug fix, refactor inside the same structure).
- Keep edits minimal: change the affected lines, don't rewrite sections, keep the same style, never add secrets.
- Mention in the final report whether `CLAUDE.md` was updated and what changed.

### 5. Final report to the user

Summarize: what was implemented, iterations run, issues found and fixed, remaining accepted issues (with reason), whether `CLAUDE.md` was updated, and the paths of the generated reports (`qa-reports/…`, `playwright-mcp/…`). Don't commit or push unless the user asks.
