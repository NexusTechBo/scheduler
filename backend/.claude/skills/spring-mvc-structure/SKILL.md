---
name: spring-mvc-structure
description: 'Package and layer structure for a Spring Boot MVC backend (controller → service/impl → repository, record DTOs, MapStruct mappers, typed exceptions). Use when creating a new Spring project, adding a new CRUD module/resource, or when asked where a class belongs.'
---

# Spring Boot MVC project structure

Before creating files, inspect the target project: base package, Spring Boot version (3.x vs 4.x), Java version (records, `var`, `_` unnamed variables in Java 22+), whether Lombok and MapStruct are installed. Adapt names; do not invent new layers.

## Package tree

```
com.example.app
├── AppApplication.java
├── config/                 # @Configuration: Security, Web (CORS + Clock), OpenApi, PasswordEncoder, WebSocket, Hibernate
│   └── tenant/             # (only with multi-tenancy) TenantContext, Resolver, ConnectionProvider
├── controller/             # @RestController, HTTP only: validate input, extract auth, delegate
├── dto/
│   └── <resource>/         # <Resource>Request, <Resource>UpdateRequest, <Resource>Response (records)
├── entity/                 # JPA @Entity (see skill jpa-entity-design)
├── enums/                  # ERole, <Something>Status
├── exception/              # ApiException + subclasses, ErrorResponse, GlobalExceptionHandler
├── mapper/                 # MapStruct @Mapper(componentModel = "spring")
├── middleware/             # servlet filters (JwtAuthFilter)
├── repository/             # JpaRepository interfaces
├── security/               # JwtUtil, ActiveUserChecker, SecurityUtils
├── service/                # <Resource>Service interfaces
│   └── impl/               # <Resource>ServiceImpl
├── util/                   # final static helpers with a private constructor
├── validator/
│   └── <name>/             # @interface + nested ConstraintValidator
└── websocket/              # events, publisher, STOMP interceptor
resources/
├── application.yaml        # everything via ${ENV_VAR}, imports .env
├── application-dev.yaml    # show-sql, format_sql, TRACE logging of binds
└── db/migration/           # Flyway (public/ and tenant/ with multi-tenancy)
```

## Rules per layer

**Controller**
- `@RestController @RequestMapping("/api/<resource-plural>") @RequiredArgsConstructor @Tag(...)`.
- Inject only the service interface. No business logic, no repositories.
- Return `ResponseEntity<DTO>`; never entities.
- Authenticated user: `Authentication auth` → `Long.parseLong(auth.getName())` and `SecurityUtils.extractRole(auth)`. See skill `spring-rest-endpoints`.

**Service**
- Interface in `service/`, implementation in `service/impl/` with `@Service @RequiredArgsConstructor`.
- `@Transactional(readOnly = true)` at class level; `@Transactional` on every method that writes.
- All business rules and ownership checks (e.g. "a MEMBER only sees their own data") live here.
- Private helpers `get<Entity>OrThrowById(id)` → `repository.findById(id).orElseThrow(() -> new ResourceNotFoundException("<Entity> not found with id: " + id))`.
- Uniqueness: check `existsBy...` before saving and throw `BusinessException` (409). The DB constraint is the final safety net (the handler maps `DataIntegrityViolationException` to 409).
- Build entities with `Entity.builder()...build()`; map the output with the mapper.
- For the current time use an injected `Clock` bean (`LocalDateTime.now(clock)`), never a bare `now()`, so tests can pin it.
- Role checks are null-safe: `ERole.MEMBER.name().equals(role)`, never `role.equals(...)`. Permission helpers are `private`.

**DTOs**
- Always `record`. Jakarta validations on the record (`@NotBlank`, `@Email`, `@Size`, `@Pattern`, custom).
- Flat responses: flatten relations (`ownerName`, `categoryName`) instead of nesting entities.
- One package per resource: `dto/user/UserRequest.java`.

**Mapper (MapStruct)**
```java
@Mapper(componentModel = "spring")
public interface ItemMapper {
    @Mapping(target = "categoryName", source = "category.name")
    ItemResponse toResponse(Item item);
    List<ItemResponse> toResponseList(List<Item> items);
}
```
Entity → response only. Creation (request → entity) is done with the builder in the service, because it usually needs to resolve relations and encode passwords.

**Exceptions**
```java
@Getter
public abstract class ApiException extends RuntimeException {
    private final HttpStatus status;
    protected ApiException(HttpStatus status, String message) { super(message); this.status = status; }
}
public class ResourceNotFoundException extends ApiException { public ResourceNotFoundException(String m) { super(HttpStatus.NOT_FOUND, m); } }
public class BadRequestException      extends ApiException { ... HttpStatus.BAD_REQUEST ... }
public class BusinessException        extends ApiException { ... HttpStatus.CONFLICT ... }
public class ForbiddenException       extends ApiException { ... HttpStatus.FORBIDDEN ... }
public class UnauthorizedException    extends ApiException { ... HttpStatus.UNAUTHORIZED ... }
```
A single `@ExceptionHandler(ApiException.class)` covers all of them. The full `GlobalExceptionHandler` is in skill `spring-essential-config`.

**Util**
```java
public final class Utils {
    private Utils() {}
    public static String normalizePhoneNumber(String s) { return StringUtils.hasText(s) ? s : null; }
}
```
With Lombok: `@NoArgsConstructor(access = AccessLevel.PRIVATE)` (not a bare `@NoArgsConstructor`, which makes it public).

## Base dependencies (Gradle, Spring Boot 4)

```groovy
implementation 'org.springframework.boot:spring-boot-starter-webmvc'      // SB3: starter-web
implementation 'org.springframework.boot:spring-boot-starter-data-jpa'
implementation 'org.springframework.boot:spring-boot-starter-validation'
implementation 'org.springframework.boot:spring-boot-starter-security'
implementation 'org.springframework.boot:spring-boot-starter-flyway'      // SB3: org.flywaydb:flyway-core
implementation 'org.springdoc:springdoc-openapi-starter-webmvc-ui:<version>'
implementation 'org.mapstruct:mapstruct:1.6.3'
compileOnly 'org.projectlombok:lombok'
runtimeOnly 'org.postgresql:postgresql'
runtimeOnly 'org.flywaydb:flyway-database-postgresql'
annotationProcessor 'org.projectlombok:lombok'
annotationProcessor 'org.projectlombok:lombok-mapstruct-binding:0.2.0'   // Lombok BEFORE MapStruct
annotationProcessor 'org.mapstruct:mapstruct-processor:1.6.3'
```

## Checklist when adding a new resource

1. Flyway migration with the table (+ named constraints).
2. `entity/<Resource>.java`.
3. `repository/<Resource>Repository.java`.
4. `dto/<resource>/` Request, (UpdateRequest), Response.
5. `mapper/<Resource>Mapper.java`.
6. `service/<Resource>Service.java` + `service/impl/<Resource>ServiceImpl.java`.
7. `controller/<Resource>Controller.java` with `@PreAuthorize` per endpoint.
8. Compile (`./gradlew compileJava`) so MapStruct generates and validates the mappings.

Related skills: `jpa-entity-design`, `spring-jpa-repository`, `spring-rest-endpoints`, `spring-essential-config`, `spring-security-jwt`.
