---
name: spring-mvc-architecture
description: Layered MVC architecture of the scheduler backend (Spring Boot 4, Java 26). Use when deciding where a new class belongs, how layers talk to each other, how to name packages and classes, or when adding a new feature slice (controller, service, repository, DTO, mapper, entity, migration).
---

# Spring MVC Architecture

The backend is a layered Spring MVC application under the base package `com.example.scheduler`. It is multi-tenant (one PostgreSQL schema per clinic), stateless (JWT) and uses Flyway for schema changes. Every new feature must fit into the existing packages; do not invent new top-level packages unless a genuinely new concern appears.

## Package map

| Package                                                       | Responsibility                                                                                               |
|---------------------------------------------------------------|--------------------------------------------------------------------------------------------------------------|
| `controller`                                                  | REST endpoints only. Receives DTOs, delegates to one service, returns `ResponseEntity` of DTOs.              |
| `service`                                                     | Service interfaces (the contract consumed by controllers and other services).                                |
| `service.impl`                                                | `*ServiceImpl` classes implementing the interfaces. All business rules live here.                            |
| `repository`                                                  | Spring Data JPA interfaces, one per aggregate root.                                                          |
| `entity`                                                      | JPA entities. Never leave this layer through a controller.                                                   |
| `enums`                                                       | Domain enums shared by entities and DTOs (`ERole`, `AppointmentStatus`, `ClinicStatus`).                     |
| `dto.<feature>`                                               | Java records grouped by feature folder: `*Request`, `*Response`, plus specific items such as `*SummaryItem`. |
| `mapper`                                                      | MapStruct interfaces converting entities to DTOs.                                                            |
| `validator.<constraintName>`                                  | Custom Bean Validation constraints, one folder per constraint.                                               |
| `exception`                                                   | `ApiException` hierarchy, `MessageKeys`, `ErrorResponse`, `GlobalExceptionHandler`, `ErrorResponseWriter`.   |
| `config`                                                      | `@Configuration` classes (security, web, locale, websocket, async, clock, Jackson, OpenAPI, Hibernate).      |
| `middleware`                                                  | Servlet filters (`JwtAuthFilter`, `RateLimitFilter`, `AccessLogFilter`).                                     |
| `security`, `security.jwt`                                    | Security helpers: role expressions, JWT utilities, current-user helpers, active user checks.                 |
| `tenant`                                                      | Tenant context, tenant resolver and schema-based connection provider.                                        |
| `websocket`, `websocket.event`                                | STOMP publisher, channel interceptor, websocket event records.                                               |
| `notification`, `notification.event`, `notification.template` | Email and WhatsApp listeners, domain events, message templates.                                              |
| `exchange`, `exchange.contract`                               | Declarative HTTP clients to external services and their contract records.                                    |
| `ratelimiting`                                                | Rate limit configuration records and enums.                                                                  |
| `tool`                                                        | Spring AI tools exposed to the chat model.                                                                   |
| `util`                                                        | Small stateless helpers and constant holders (`LogKeys`, `TokenUtils`).                                      |

## Request flow

1. Filters run first: access log, JWT authentication (populates the security context and the tenant), rate limiting.
2. The controller validates the input with `@Valid` and delegates to exactly one service method.
3. The service implementation applies business rules inside a transaction, uses repositories, publishes domain events through `ApplicationEventPublisher`, and returns DTOs produced by a mapper.
4. Listeners react to events after commit (websocket board updates, email, WhatsApp).
5. Any failure is an `ApiException` subclass or a framework exception translated by `GlobalExceptionHandler` into a localized `ErrorResponse`.

## Layer rules

- Controllers: annotate with `@RestController`, `@RequestMapping("/api/<resource>")`, `@RequiredArgsConstructor` and `@Tag`. Each endpoint has `@PreAuthorize` using a constant from `RoleExpressions` and an `@Operation` summary. No business logic, no repository access, no try/catch, no entity types in signatures.
- Services: the controller depends on the interface, never on the `Impl`. Implementations use `@Service`, `@RequiredArgsConstructor` and class-level `@Transactional(readOnly = true)`; write methods add `@Transactional`. A service may depend on other services' interfaces but avoid cycles; extract a collaborator when two services need the same logic.
- Repositories: only data access; no business decisions. See the `spring-jpa` skill.
- DTOs: immutable records, validation annotations on request records, never expose entity references. A response record holds flat values or nested response records.
- Mapping: always through a MapStruct mapper. See the `spring-mappers` skill.
- Errors: throw the specific `ApiException` subclass with a `MessageKeys` constant. See the `spring-exceptions` skill.
- Time: inject the application `Clock` bean instead of calling `now()` without a clock, so services stay testable.
- Tenant: never pass the schema around manually; rely on `TenantContext`, which is set by the authentication filter and the websocket interceptor.
- Cross-cutting side effects (notifications, realtime updates) are triggered by publishing events, never by calling the listener directly.

## Naming

- Controllers `<Resource>Controller`, services `<Resource>Service` and `<Resource>ServiceImpl`, repositories `<Entity>Repository`, mappers `<Entity>Mapper`.
- Request records end in `Request`, response records in `Response`; action-specific requests describe the action (for example cancelling or activating).
- Endpoint paths are plural nouns in kebab case under `/api`; actions that are not CRUD use a sub-resource verb path on the specific item.
- Message keys follow `<feature>.<camelCaseReason>` and live in `MessageKeys`.

## Adding a feature slice (checklist)

1. New Flyway migration in the right folder (`db/migration/public` for global tables, `db/migration/tenant` for per-clinic tables). Never edit an applied migration.
2. Entity and enums.
3. Repository methods with the right fetch strategy.
4. Request and response records in `dto.<feature>`, with validation.
5. Mapper.
6. Service interface and implementation, with message keys added to `MessageKeys`, `messages.properties` and `messages_es.properties`.
7. Controller with security and OpenAPI annotations.
8. If the endpoint is sensitive or public, add a rate limit policy in `application.yaml`; if it is public, add it to `security.public-paths`.
9. Run `gradlew.bat spotlessApply` and `gradlew.bat build` from `backend`.
