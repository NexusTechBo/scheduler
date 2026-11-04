# Scheduler (ClinicFlow)

Multi-tenant medical appointment scheduler. Each clinic is a tenant with its own PostgreSQL schema. Staff (administrators, doctors, assistants) manage appointments, availability, patients and staff; patients can book as guests through a public link; changes are pushed in realtime and notified by email and WhatsApp.

## Project structure

| Path                 | Description                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                  |
|----------------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `backend/`           | Spring Boot 4, Java 26, Gradle. Layered MVC under `com.example.scheduler`: `controller`, `service` + `service/impl`, `repository`, `entity`, `dto/<feature>`, `mapper` (MapStruct), `validator`, `exception`, `config`, `middleware` (JWT, rate limit, access log), `security`, `tenant`, `websocket`, `notification`, `exchange` (WhatsApp service client), `ratelimiting`, `tool` (Spring AI). Flyway migrations in `src/main/resources/db/migration/{public,tenant}`; messages in `messages*.properties`. |
| `frontend/`          | Angular 22 standalone app with signals, Angular CDK and Tailwind 4. `src/app/core` (API classes, auth, catalog stores, HTTP interceptors, models, navigation, utils), `features/<feature>` (page, feature service, model, private components), `shared` (UI primitives, shared domain components, services, pipes), `layout/shell`. Unit tests with Vitest.                                                                                                                                                  |
| `docker-compose.yml` | Backend on 8080 and frontend (nginx) on 4200; PostgreSQL is expected on the host.                                                                                                                                                                                                                                                                                                                                                                                                                            |
| `.claude/agents/`    | Development, review and testing agents (see the workflow below).                                                                                                                                                                                                                                                                                                                                                                                                                                             |
| `.claude/skills/`    | Project conventions. Backend: `spring-mvc-architecture`, `spring-jpa`, `spring-multitenancy`, `spring-mappers`, `spring-validation`, `spring-exceptions`, `spring-i18n`, `spring-websocket`, `spring-rate-limiting`, `spring-security`, `spring-logging`. Frontend: `angular-architecture`, `angular-component-modularization`, `angular-developer`.                                                                                                                                                         |

## Commands

- Backend (from `backend/`): `gradlew.bat spotlessApply`, `gradlew.bat build`, `gradlew.bat test`, `gradlew.bat bootRun` (needs `backend/.env` and PostgreSQL).
- Frontend (from `frontend/`): `npm start` (port 4200, proxies `/api` and `/ws` to 8080), `npm run build`, `npm run test:ci`, `npm run format`.

## Agents

| Agent                       | Role                                                                                          |
|-----------------------------|-----------------------------------------------------------------------------------------------|
| `spring-feature-developer`  | Implements new backend requirements.                                                          |
| `angular-feature-developer` | Implements new frontend requirements.                                                         |
| `spring-reviewer`           | Read-only review of backend changes: SOLID, design patterns, modularity, project conventions. |
| `angular-reviewer`          | Read-only review of frontend changes: same criteria for components, services and stores.      |
| `spring-tester`             | Unit tests for services and controllers (JUnit 5, Mockito, MockMvc).                          |
| `angular-tester`            | Unit tests for feature services, components and core pieces (Vitest, TestBed).                |
| `ui-e2e-tester`             | End-to-end test cases from the browser with the Playwright MCP against the running app.       |

## Development workflow

Every task that implements or changes a feature follows this loop. The main session is the orchestrator: subagents cannot launch other agents, so the main session launches each one, passes context between them and decides when the loop ends.

### 1. Analysis (main session)

- Read the request and the affected code. Split it into concrete requirements with acceptance criteria.
- Classify each requirement as backend, frontend or both, and define the API contract (endpoints, request and response fields, errors, roles) before any implementation when both sides are involved.
- If something is ambiguous and changes the design, ask the user before continuing.
- Present the short task breakdown to the user, then continue.

### 2. Implementation

- Launch `spring-feature-developer` and/or `angular-feature-developer` with their requirements, acceptance criteria and the shared contract. If both are needed and the contract is fixed, run them in parallel; otherwise backend first.
- Each developer must leave the code building (`gradlew.bat build -x test`, `npm run build`) and report the files changed.

### 3. Verification (in parallel)

Launch in a single step, in parallel, only the agents relevant to the sides that changed:

- `spring-reviewer` and/or `angular-reviewer`, with the list of changed files and the requirements.
- `spring-tester` and/or `angular-tester`, with the classes and components to cover.
- `ui-e2e-tester`, with the acceptance criteria as test cases, when the change is visible in the UI and the app is running (backend on 8080, frontend on 4200). If the app is not running, ask the user to start it or skip this agent and say so.

### 4. Consolidation (main session)

Merge all results into a single list of observations, removing duplicates:

- Reviewer findings: Critical and Major must be fixed; Minor is fixed when cheap; Nit is optional.
- Failing unit tests: if the failure exposes a production bug, it goes to the developer; if the test itself is wrong, it goes back to the tester.
- E2E failures: assigned to the backend or frontend developer according to the evidence.

### 5. Loop

- If there are observations to fix, send them to the corresponding developer (and to the tester for broken tests), then repeat step 3 only for the affected sides, giving the reviewers the previous findings so they verify each one.
- The loop ends when there are no Critical or Major findings, all unit tests pass and all E2E cases pass or are justified as blocked.
- Safety limit: at most 5 iterations. If observations remain after that, or the same observation reappears twice, stop and ask the user how to proceed.

### 6. Final report (main session)

Summarize for the user: requirements implemented, files changed, tests added and their results, E2E results, observations fixed, observations accepted or pending, and any manual step (migrations, environment variables). Do not commit or push unless the user asks.

### When not to run the full loop

Questions, explanations, small fixes of a few lines, configuration or documentation changes are handled directly in the main session without agents. For a bug fix, use the matching developer plus the matching tester to reproduce it with a test first, and a reviewer only when the fix touches design.

## General rules

- Follow the project skills; when existing code differs from a skill, existing code wins and the difference is reported.
- Never edit an applied Flyway migration; never hardcode secrets; every user-facing backend message is a key in both message bundles; UI text is in Spanish.
- Do not commit or push without an explicit request.
