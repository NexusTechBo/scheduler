---
name: spring-multi-tenancy
description: 'Schema-per-tenant multi-tenancy (one PostgreSQL schema per tenant) in Spring Boot + Hibernate 6/7 + Flyway: ThreadLocal TenantContext, CurrentTenantIdentifierResolver, MultiTenantConnectionProvider with search_path, schema provisioning with programmatic Flyway, tenant onboarding and resolving the tenant from the JWT. Use when adding tenant/customer/organization isolation, creating per-tenant schemas, or debugging queries that hit the wrong schema.'
---

# Schema-per-tenant multi-tenancy

Model: a `public` schema with global data (the tenant registry: a `tenants` table with `id`, `name`, unique `subdomain`) + one `tenant_<id>` schema per tenant with identical tables. Hibernate asks for a connection per tenant and the provider runs `SET search_path TO tenant_<id>, public`. Tenant-scoped entities do **not** declare a schema; global ones use `@Table(schema = "public")`.

Pick a prefix (`tenant_`, `org_`, `company_`...) and use it the same way everywhere. Below: `tenant_`.

## 1. application.yaml

```yaml
spring:
  jpa:
    hibernate:
      ddl-auto: none
    open-in-view: false
    properties:
      hibernate:
        multiTenancy: SCHEMA          # informational in Hibernate 6+; the provider is what enables it
  flyway:
    locations: classpath:db/migration/public   # Spring only migrates public
    baseline-on-migrate: true
```

## 2. TenantContext

```java
public class TenantContext {
    private static final ThreadLocal<String> CURRENT_TENANT = new ThreadLocal<>();
    public static void setCurrentTenant(String tenantId) { CURRENT_TENANT.set(tenantId); }
    public static String getCurrentTenant() { return CURRENT_TENANT.get(); }
    public static void clear() { CURRENT_TENANT.remove(); }
}
```
Rule: every `set` is followed by `try { ... } finally { TenantContext.clear(); }`.

## 3. Resolver

```java
@Component
public class CustomTenantResolver implements CurrentTenantIdentifierResolver<String> {
    private static final String DEFAULT_TENANT = "public";

    @Override
    public String resolveCurrentTenantIdentifier() {
        var tenant = TenantContext.getCurrentTenant();
        return (tenant != null && !tenant.isBlank()) ? tenant : DEFAULT_TENANT;
    }

    @Override
    public boolean validateExistingCurrentSessions() { return true; }
}
```

## 4. Connection provider

```java
@Component
@RequiredArgsConstructor
public class SchemaBasedMultiTenantConnectionProvider implements MultiTenantConnectionProvider<String> {
    private final DataSource dataSource;

    @Override public Connection getAnyConnection() throws SQLException {
        var c = dataSource.getConnection(); setSearchPath(c, "public"); return c;
    }
    @Override public void releaseAnyConnection(Connection c) throws SQLException { c.close(); }

    @Override public Connection getConnection(String tenantIdentifier) throws SQLException {
        var c = dataSource.getConnection();
        setSearchPath(c, tenantIdentifier + ", public");     // public last: global tables stay visible
        return c;
    }
    @Override public void releaseConnection(String tenantIdentifier, Connection c) throws SQLException {
        setSearchPath(c, "public");                           // reset before returning to the pool
        c.close();
    }

    @Override public boolean supportsAggressiveRelease() { return false; }
    @Override public boolean isUnwrappableAs(Class<?> t) { return false; }
    @Override public <T> T unwrap(Class<T> t) { throw new UnsupportedOperationException("Unwrap not supported"); }

    private void setSearchPath(Connection c, String path) throws SQLException {
        try (Statement st = c.createStatement()) { st.execute("SET search_path TO " + path); }
    }
}
```
`SET search_path` does not accept JDBC parameters: the identifier may **only** come from `"tenant_" + <Long>` built on the server (numeric JWT claim or DB id), never from client text.

## 5. Register it in Hibernate

```java
@Configuration
@RequiredArgsConstructor
public class HibernateConfig implements HibernatePropertiesCustomizer {
    private final SchemaBasedMultiTenantConnectionProvider connectionProvider;
    private final CustomTenantResolver tenantResolver;

    @Override
    public void customize(Map<String, Object> props) {
        props.put(AvailableSettings.MULTI_TENANT_CONNECTION_PROVIDER, connectionProvider);
        props.put(AvailableSettings.MULTI_TENANT_IDENTIFIER_RESOLVER, tenantResolver);
    }
}
```
`HibernatePropertiesCustomizer` import: SB4 `org.springframework.boot.hibernate.autoconfigure`, SB3 `org.springframework.boot.autoconfigure.orm.jpa`.

## 6. Migrations

```
resources/db/migration/
├── public/V1__init_public_schema.sql     # CREATE TABLE IF NOT EXISTS public.tenants (...)
└── tenant/V1__init_tenant_schema.sql     # uses ${flyway:defaultSchema}
```
```sql
CREATE TABLE IF NOT EXISTS ${flyway:defaultSchema}.users (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(255) NOT NULL,
    CONSTRAINT uq_${flyway:defaultSchema}_users_email UNIQUE (email)   -- constraint name includes the schema
);
```
Each tenant schema has its own `flyway_schema_history`.

## 7. Schema provisioning

```java
@Service
@RequiredArgsConstructor
public class SchemaProvisioningServiceImpl implements SchemaProvisioningService, ApplicationRunner {
    private static final String TENANT_MIGRATIONS = "classpath:db/migration/tenant";
    private final DataSource dataSource;
    private final JdbcTemplate jdbcTemplate;

    @Override   // on startup: migrate every existing tenant (applies new migrations)
    public void run(@NonNull ApplicationArguments args) {
        jdbcTemplate.queryForList("SELECT id FROM tenants", Long.class)
                .forEach(id -> createTenantSchema("tenant_" + id));
    }

    @Override
    public void createTenantSchema(String schemaName) {
        validateSchemaName(schemaName);
        Flyway.configure()
                .dataSource(dataSource)
                .schemas(schemaName)              // creates the schema if missing and uses it as defaultSchema
                .locations(TENANT_MIGRATIONS)
                .baselineOnMigrate(true)
                .load()
                .migrate();
    }

    private void validateSchemaName(String schemaName) {
        if (schemaName == null || !schemaName.matches("^tenant_\\d+$"))
            throw new IllegalArgumentException("Invalid tenant schema name: " + schemaName);
    }
}
```

## 8. Tenant onboarding (no @Transactional on the method)

The tenant is saved in `public`, the schema is created (DDL, outside a JPA transaction), and then the admin is created **inside** the new schema:

```java
public TenantCreatedResponse createTenant(TenantRequest request) {
    // existsBy... checks in public
    var tx = new TransactionTemplate(transactionManager);
    var tenant = tx.execute(_ -> tenantRepository.save(Tenant.builder()...build()));
    var schemaName = "tenant_" + tenant.getId();
    schemaProvisioningService.createTenantSchema(schemaName);
    try {
        TenantContext.setCurrentTenant(schemaName);    // BEFORE opening the tx: Hibernate resolves the tenant when the session opens
        return tx.execute(_ -> {
            var admin = userRepository.save(User.builder()...role(ERole.ADMINISTRATOR).build());
            return new TenantCreatedResponse(...);
        });
    } finally {
        TenantContext.clear();
    }
}
```
Why `TransactionTemplate` and not `@Transactional`: a Hibernate session's tenant is fixed when it opens; changing `TenantContext` mid-transaction does not switch schema.

## 9. Where does the tenant come from?

| Moment                    | Source                                                                                      | Where it is set                                                      |
|---------------------------|---------------------------------------------------------------------------------------------|----------------------------------------------------------------------|
| Authenticated request     | `tenantId` JWT claim                                                                        | `JwtAuthFilter` (before `ActiveUserChecker`), `clear()` in `finally` |
| Login                     | username suffix (`john@acme.com` → `tenants.subdomain = '@acme.com'`) looked up in `public` | `AuthService.login`, `try/finally`                                   |
| STOMP CONNECT             | JWT claim                                                                                   | `JwtChannelInterceptor`, store `tenantId` in session attributes      |
| Seeders / jobs / `@Async` | id of the tenant being iterated                                                             | manually with `try/finally` + `TransactionTemplate`                  |

Getting the current id in a service: `Long.valueOf(TenantContext.getCurrentTenant().substring("tenant_".length()))`.

## 10. Data seeder

```java
if (tenantRepository.count() > 0) return;
var t = tenantRepository.save(...);
schemaProvisioningService.createTenantSchema("tenant_" + t.getId());
var tx = new TransactionTemplate(transactionManager);
try {
    TenantContext.setCurrentTenant("tenant_" + t.getId());
    tx.executeWithoutResult(_ -> seedTenant(t));
} finally { TenantContext.clear(); }
```
Watch the `ApplicationRunner` order: provisioning must run before the seeder if the seeder assumes the schemas exist (or create the schema explicitly as above).

## Common mistakes

- Query hits `public` → `TenantContext` is empty on that thread (`@Async`, scheduler, `AFTER_COMMIT` listener). Propagate the tenant manually.
- Leak between tenants → missing `clear()` in `finally` (pool threads are reused).
- `relation "x" does not exist` → schema not provisioned, or a new tenant migration without a restart.
- Native query with a hardcoded `public.` or `tenant_1.` → breaks isolation; never prefix tenant tables.
