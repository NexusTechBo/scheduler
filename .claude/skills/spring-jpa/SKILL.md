---
name: spring-jpa
description: JPA, Hibernate and Spring Data conventions in the scheduler backend - entity modeling, fetching strategy, N+1 prevention, locking, transactions, multi-tenant schemas and Flyway migrations. Use when creating entities, repository methods, queries or migrations.
---

# Spring JPA

PostgreSQL with Hibernate, schema-based multi-tenancy, `open-in-view` disabled and `ddl-auto` set to none. Flyway is the only way the schema changes.

## Entities

- Lombok: `@Getter`, `@Setter`, `@NoArgsConstructor`, `@AllArgsConstructor`, `@Builder`, and `@EqualsAndHashCode(of = "id")`. Never use `@Data` or `@ToString` that includes relations.
- Identity primary keys (`GenerationType.IDENTITY`) of type `Long`.
- Every `@ManyToOne` and `@OneToOne` is `fetch = LAZY`; mark `optional = false` when the column is not nullable. Collections are lazy by default and should be rare; prefer querying from the owning side.
- Enums stored with `EnumType.STRING`.
- Defaults for builder-created entities use `@Builder.Default`.
- Entities that can be edited concurrently carry a `@Version` column for optimistic locking.
- Creation timestamps are set in a `@PrePersist` callback and are not updatable.
- Column constraints (nullable, length, unique) mirror the migration; the migration is the source of truth.

## Repositories

Choose the simplest option that loads exactly what the endpoint needs:

1. Built-in methods for plain lookups by id or save.
2. Derived query methods for simple filters and existence checks (`existsBy...`, `findBy...`). Prefer `exists` over loading an entity just to check presence.
3. `@Query` in JPQL with `JOIN FETCH` when the endpoint needs relations, or when filters are optional (the project uses the "parameter is null or matches" pattern for optional filters).
4. `@EntityGraph(attributePaths = ...)` to add fetches to a derived or overridden method, for example overriding `findById` to fetch the relations every detail endpoint uses.
5. Pessimistic locking (`@Lock(PESSIMISTIC_WRITE)`) only for check-then-act sequences that must be serialized, such as preventing double booking of a doctor slot.

Additional rules:

- Never fetch a collection together with pagination in the same query; paginate ids or use a count query.
- Use `Page` with `Pageable` for list endpoints that can grow; validate sortable properties (the global handler turns invalid sort properties into 400).
- Always pass the excluded or included statuses as parameters instead of string literals when the list can change.
- Overlap checks use the half-open interval rule: existing start before new end and existing end after new start.
- Annotate overridden Spring Data methods with the JSpecify null-marking used in the project.

## Transactions

- Service classes are `@Transactional(readOnly = true)` by default; write methods add `@Transactional`.
- Never open transactions in controllers. With `open-in-view` disabled, any lazy access outside the service transaction fails, which is intended.
- Side effects that must happen only after a successful commit go through events and `@TransactionalEventListener(phase = AFTER_COMMIT)`.
- Async work runs on the configured executor with the MDC task decorator; set the tenant explicitly inside async tasks if they touch the database.

## Multi-tenancy

Summary only; the full rules live in the `spring-multitenancy` skill.

- Global tables (clinics, activation tokens) live in the `public` schema; clinic data lives in one schema per clinic.
- The tenant is resolved from `TenantContext`, set by the JWT filter and cleared at the end of the request. Code outside a request (listeners, schedulers, seeders) must set and clear it explicitly.
- New tenant schemas are provisioned by `SchemaProvisioningService`, which runs the tenant migrations.

## Migrations

- Global changes go to `db/migration/public`, per-clinic changes to `db/migration/tenant`, with the next version number and a descriptive snake case name.
- Never modify an applied migration; always add a new one.
- Add indexes for foreign keys and for columns used in frequent filters and overlap checks.
- Data backfills belong in the same migration as the column change when they are required for the new constraint.
