---
name: spring-multitenancy
description: Schema-per-tenant multi-tenancy in the scheduler backend (one PostgreSQL schema per clinic, Hibernate multi-tenant connection provider, ThreadLocal tenant context). Use when touching clinic-scoped data outside a normal request, switching tenants, provisioning or dropping clinic schemas, writing tenant migrations, async listeners, schedulers, seeders, websocket code, or when reviewing code for cross-tenant data leaks.
---

# Spring Multi-Tenancy (schema per clinic)

Each clinic is a tenant with its own PostgreSQL schema named `clinic_<slug>`. Global data (the clinics themselves and their activation tokens) lives in the `public` schema. The same JPA entities and repositories serve every clinic; the active schema is chosen per database connection from the current tenant.

## Building blocks

| Class                                             | Role                                                                                                                                                                                                                                                 |
|---------------------------------------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `tenant.Tenant`                                   | Immutable record with the clinic id and the clinic slug.                                                                                                                                                                                             |
| `tenant.TenantContext`                            | ThreadLocal holder of the current tenant. Builds schema names from slugs, exposes the current tenant, the current schema and the current clinic id, supports set, restore and clear, and mirrors clinic id and slug into the logging MDC.            |
| `tenant.CustomTenantResolver`                     | Hibernate tenant identifier resolver. Returns the current clinic schema, or `public` when no tenant is bound. Validates that an open session keeps the same tenant.                                                                                  |
| `tenant.SchemaBasedMultiTenantConnectionProvider` | Hibernate connection provider. For a tenant it sets the PostgreSQL search path to the clinic schema followed by `public`; for no tenant only `public`; on release it resets the search path to `public` before returning the connection to the pool. |
| `config.HibernateConfig`                          | Registers the resolver and the connection provider in the Hibernate properties. The `multiTenancy: SCHEMA` property in `application.yaml` enables the strategy.                                                                                      |
| `service.SchemaProvisioningService`               | Creates a clinic schema by running the tenant Flyway migrations on it, drops a clinic schema, and at startup migrates the schemas of every existing clinic. Validates the schema name before any DDL.                                                |

## Where the tenant comes from

- HTTP requests: `JwtAuthFilter` reads the clinic id and slug from the JWT claims, binds the tenant before checking that the user is active, and always clears it in a finally block when the request ends. Unauthenticated requests run with no tenant, so they only see `public`.
- WebSocket: `JwtChannelInterceptor` binds the tenant only for the duration of the CONNECT validation and clears it immediately; the clinic id is then kept in the STOMP session attributes, not in the thread.
- Login: `AuthServiceImpl` finds the clinic in `public` by the slug embedded in the username, then switches to that clinic's tenant to look up the user, and restores the previous tenant afterwards.
- Clinic registration: `ClinicServiceImpl` saves the clinic in `public`, provisions the schema, switches to the new tenant to create the administrator in a separate transaction, and on failure clears the tenant and removes the clinic and its schema.
- Seeder: sets and clears the tenant around each clinic's data.

## Rules

1. Never bind a tenant without guaranteeing cleanup. Thread pools reuse threads; a leaked tenant means the next request on that thread reads another clinic's data. Always pair a set with a clear or a restore in a finally block.
2. When switching tenants inside code that may already have one (login, activation, any nested call), capture the current tenant first and restore it afterwards instead of clearing.
3. Never switch tenants inside an open transaction. The connection, and therefore the search path, is fixed when the transaction starts, and the resolver rejects tenant changes for an existing session. Bind the tenant first, then start the transaction (with a separate service call or a `TransactionTemplate`), as the registration flow does.
4. Do not pass schema names or clinic ids around to select data. Repositories are tenant-agnostic; the tenant decides the schema. Use `TenantContext` only where the clinic id is needed as a value (topic names, external session ids, event payloads).
5. Do not store `clinic_id` columns in tenant tables to filter data; isolation comes from the schema. Global tables in `public` are the only ones that reference clinics by id.
6. Entities that live in `public` and entities that live in clinic schemas must never be joined in the same JPQL query or mapped as JPA relations to each other. Load global data and tenant data separately.
7. Because the search path falls back to `public`, an unqualified table missing from the clinic schema silently resolves to a `public` table with the same name. Keep table names unique across the two migration sets.
8. Schema names are built only through `TenantContext` from a validated slug (lowercase letters and digits). Never build DDL from raw user input; any new DDL must go through the provisioning service's name validation.
9. `@Async` executors propagate the logging MDC, not the tenant. Asynchronous listeners must not depend on the thread's tenant. Prefer events that carry everything the listener needs (as the notification events already do, including clinic name and slug). If a listener must query clinic data, bind the tenant from the event's clinic id and slug and clear it in a finally block.
10. Scheduled jobs and startup runners have no tenant. Iterate clinics from `public` and bind and clear the tenant for each clinic, isolating failures so one clinic does not stop the others.
11. Caches that hold clinic data must include the clinic id in the key, or be cleared per tenant. Never cache tenant data under a global key.
12. Code that requires a tenant should fail fast (`getCurrentClinicId` throws when no tenant is bound) instead of falling back to `public` silently.

## Migrations

- `db/migration/public`: run by Spring Boot's Flyway on startup against `public`. Only global tables.
- `db/migration/tenant`: run by the provisioning service on each clinic schema, both for new clinics and for every existing clinic at startup. Every tenant migration must be safe to run on schemas with real data and must not reference objects in another clinic's schema.
- Never edit an applied migration in either folder; add a new version.
- A failed tenant migration on startup affects every clinic. Test tenant migrations against a schema with data before merging.

## Adding clinic-scoped functionality (checklist)

1. Decide whether the data is global (`public`) or per clinic (tenant schema) and put the migration in the matching folder.
2. If the code runs inside an authenticated request, do nothing special: the tenant is already bound.
3. If it runs anywhere else (public endpoint resolving a clinic, async listener, scheduler, runner, websocket), bind the tenant explicitly, start transactions only after binding, and clear or restore in a finally block.
4. If the code needs the clinic id as a value, read it from `TenantContext` and include it in events published for after-commit and async processing.
5. Review for leaks: every set has a matching clear or restore, and no cache or static field holds tenant data without the clinic in the key.

## Testing

- Unit tests for code that reads the tenant: bind a tenant in the arrange step and clear it in an after-each hook so tests never leak state to each other.
- Test that tenant-switching services restore the previous tenant on success and on exception.
- The provisioning service's schema name validation deserves direct tests with valid and malicious names.
- Real schema isolation (two clinics, data visible only in their own schema) requires PostgreSQL and belongs in integration tests, not unit tests.
