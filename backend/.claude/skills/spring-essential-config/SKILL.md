---
name: spring-essential-config
description: 'Essential configuration for a Spring Boot backend: application.yaml with environment variables and .env, dev profile, CORS, Swagger/OpenAPI with Bearer JWT, PasswordEncoder, Clock with time zone, global error handling (ErrorResponse + GlobalExceptionHandler), Flyway and Dockerfile. Use when starting a project, configuring CORS or Swagger, adding error handling or environment variables.'
---

# Essential configuration

## 1. application.yaml (everything via environment variables)

```yaml
spring:
  config:
    import: optional:file:.env[.properties]     # reads a local .env with no extra libraries
  application:
    name: app
  datasource:
    url: ${DB_URL}
    username: ${DB_USERNAME}
    password: ${DB_PASSWORD}
    driver-class-name: org.postgresql.Driver
  jpa:
    hibernate:
      ddl-auto: none          # the schema is owned by Flyway, never by Hibernate
    open-in-view: false       # mandatory: forces explicit fetching in repositories
    properties:
      hibernate:
        dialect: org.hibernate.dialect.PostgreSQLDialect
  flyway:
    locations: classpath:db/migration    # with multi-tenancy: classpath:db/migration/public
    baseline-on-migrate: true

server:
  port: ${PORT:8080}

cors:
  allowed-origins: ${CORS_ALLOWED_ORIGINS}

jwt:
  secret: ${JWT_SECRET}

security:
  public-paths: /api/auth/login, /swagger-ui/**, /v3/api-docs/**

app:
  zone: ${APP_ZONE:America/La_Paz}
```

`application-dev.yaml` (enable with `SPRING_PROFILES_ACTIVE=dev`):
```yaml
spring:
  jpa:
    show-sql: true
    properties:
      hibernate:
        format_sql: true
logging:
  level:
    org.hibernate.orm.jdbc.bind: TRACE
```

`.env.example` is versioned, `.env` goes in `.gitignore`:
```
DB_URL=jdbc:postgresql://localhost:5432/app
DB_USERNAME=postgres
DB_PASSWORD=
PORT=8080
CORS_ALLOWED_ORIGINS=http://localhost:4200
JWT_SECRET=change-me-in-production-minimum-32-chars!
APP_ZONE=America/La_Paz
```

## 2. WebConfig: CORS + Clock

```java
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Value("${cors.allowed-origins:*}")
    private String[] allowedOrigins;               // comma-separated list in the env var

    @Bean
    public Clock clock(@Value("${app.zone:UTC}") String zone) {
        return Clock.system(ZoneId.of(zone));      // inject in services: LocalDateTime.now(clock)
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins(allowedOrigins)
                .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(false)            // JWT in a header, no cookies; must be false with "*"
                .maxAge(3600);
    }
}
```
For Spring Security to honor this config: `.cors(Customizer.withDefaults())` in the `SecurityFilterChain` (see `spring-security-jwt`).

## 3. Swagger / OpenAPI with Bearer

```java
@Configuration
public class OpenApiConfig {
    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info().title("App API").version("v1"))
                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"))
                .components(new Components()
                        .addSecuritySchemes("bearerAuth", new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")));
    }
}
```
Dependency `org.springdoc:springdoc-openapi-starter-webmvc-ui` (v3.x for Spring Boot 4, v2.x for SB3). UI at `/swagger-ui/index.html`; remember `/swagger-ui/**, /v3/api-docs/**` in `security.public-paths`.

## 4. PasswordEncoder

```java
@Configuration
public class PasswordEncoderConfig {
    @Bean
    public PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(); }
}
```
Kept separate from `SecurityConfig` to avoid dependency cycles (services → encoder, security → filter → services).

## 5. Global error handling

```java
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse(int status, String error, String message, String path,
                            LocalDateTime timestamp, List<FieldErrorItem> errors) {
    public record FieldErrorItem(String field, String message) {}

    public static ErrorResponse of(HttpStatus status, String message, String path) { return of(status, message, path, null); }
    public static ErrorResponse of(HttpStatus status, String message, String path, List<FieldErrorItem> errors) {
        return new ErrorResponse(status.value(), status.getReasonPhrase(), message, path, LocalDateTime.now(), errors);
    }
}
```

```java
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ApiException.class)                              // all business exceptions
    public ResponseEntity<ErrorResponse> handleApi(ApiException ex, HttpServletRequest req) {
        return build(ex.getStatus(), ex.getMessage(), req);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)           // @Valid @RequestBody
    public ResponseEntity<ErrorResponse> handleBodyValidation(MethodArgumentNotValidException ex, HttpServletRequest req) {
        var errors = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> new ErrorResponse.FieldErrorItem(e.getField(), e.getDefaultMessage())).toList();
        return validationFailed(errors, req);
    }

    @ExceptionHandler(HandlerMethodValidationException.class)          // constraints on @RequestParam/@PathVariable
    public ResponseEntity<ErrorResponse> handleParamValidation(HandlerMethodValidationException ex, HttpServletRequest req) {
        var errors = ex.getParameterValidationResults().stream()
                .flatMap(r -> r.getResolvableErrors().stream()
                        .map(e -> new ErrorResponse.FieldErrorItem(r.getMethodParameter().getParameterName(), e.getDefaultMessage())))
                .toList();
        return validationFailed(errors, req);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolation(ConstraintViolationException ex, HttpServletRequest req) {
        var errors = ex.getConstraintViolations().stream()
                .map(v -> new ErrorResponse.FieldErrorItem(v.getPropertyPath().toString(), v.getMessage())).toList();
        return validationFailed(errors, req);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrity(DataIntegrityViolationException ex, HttpServletRequest req) {
        log.warn("Data integrity violation on {}: {}", req.getRequestURI(), ex.getMostSpecificCause().getMessage());
        return build(HttpStatus.CONFLICT, "The operation conflicts with existing data", req);   // never leak SQL to the client
    }

    @ExceptionHandler(InvalidDataAccessApiUsageException.class)
    public ResponseEntity<ErrorResponse> handleInvalidApiUsage(InvalidDataAccessApiUsageException ex, HttpServletRequest req) {
        if (ex.contains(SemanticException.class)) return build(HttpStatus.BAD_REQUEST, "Invalid sort property", req);
        return handleUnexpected(ex, req);
    }

    // The rest: one method per exception with the same skeleton (return build(STATUS, "message", req)):
    //   HttpMessageNotReadableException          → 400 "Malformed request body"
    //   MethodArgumentTypeMismatchException      → 400 "Invalid value for parameter '" + ex.getName() + "'"
    //   MissingServletRequestParameterException  → 400 "Missing required parameter '" + ex.getParameterName() + "'"
    //   PropertyReferenceException               → 400 "Invalid sort property '" + ex.getPropertyName() + "'"
    //   NoResourceFoundException                 → 404 "Resource not found"
    //   HttpRequestMethodNotSupportedException   → 405 "Method " + ex.getMethod() + " not supported"
    //   HttpMediaTypeNotSupportedException       → 415 "Unsupported content type"
    //   OptimisticLockingFailureException        → 409 "The resource was modified by another request, retry"
    //   AccessDeniedException (@PreAuthorize)    → 403 "Access denied"

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception ex, HttpServletRequest req) {
        log.error("Unhandled exception on {} {}", req.getMethod(), req.getRequestURI(), ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error", req);   // never expose ex.getMessage()
    }

    private ResponseEntity<ErrorResponse> validationFailed(List<ErrorResponse.FieldErrorItem> errors, HttpServletRequest req) {
        return ResponseEntity.badRequest().body(ErrorResponse.of(HttpStatus.BAD_REQUEST, "Validation failed", req.getRequestURI(), errors));
    }

    private ResponseEntity<ErrorResponse> build(HttpStatus status, String message, HttpServletRequest req) {
        return ResponseEntity.status(status).body(ErrorResponse.of(status, message, req.getRequestURI()));
    }
}
```
Imports that change in Spring Boot 4: `org.springframework.data.core.PropertyReferenceException` (SB3: `org.springframework.data.mapping`), Jackson 3 `tools.jackson.databind.ObjectMapper` (SB3: `com.fasterxml.jackson.databind`). The `com.fasterxml.jackson.annotation.*` annotations stay the same.

401/403 errors raised **before** the controller (security filter) do not go through here: they are written with the same `ErrorResponse` from `SecurityConfig` (see `spring-security-jwt`).

## 6. Flyway

- `ddl-auto: none`; every schema change = a new `V<n>__description.sql`. Never edit an applied migration.
- Named constraints (`CONSTRAINT uq_users_email UNIQUE (email)`) so they can be diagnosed.
- With schema multi-tenancy: `db/migration/public` (Spring) and `db/migration/tenant` (programmatic), see `spring-multi-tenancy`.

## 7. Dockerfile (multi-stage)

```dockerfile
FROM eclipse-temurin:<java>-jdk AS build
WORKDIR /app
COPY . .
RUN ./gradlew bootJar --no-daemon -x test

FROM eclipse-temurin:<java>-jre
WORKDIR /app
COPY --from=build /app/build/libs/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```
Variables (`DB_URL`, `JWT_SECRET`, ...) are injected by the platform; never copy `.env` into the image (add it to `.dockerignore`).
