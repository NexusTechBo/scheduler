---
name: spring-security
description: Stateless JWT security in the scheduler backend (Spring Security filter chain, roles, method security, guest tokens, CORS, password hashing). Use when adding or protecting an endpoint, making a path public, adding a role or claim, reading the current user, restricting doctors to their own data, or debugging 401/403 responses.
---

# Spring Security

The backend is a stateless REST API secured with HMAC-signed JWTs. There are no sessions, cookies, CSRF tokens, form login or HTTP Basic. Authorization is enforced twice: coarsely by path in the filter chain and precisely per endpoint with method security.

## Building blocks

| Class                           | Role                                                                                                                                                                                                                 |
|---------------------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `config.SecurityConfig`         | The single `SecurityFilterChain`: stateless sessions, CSRF/Basic/form login disabled, CORS from `WebConfig`, path rules, JSON 401/403 handlers, filter ordering. Enables method security.                            |
| `middleware.JwtAuthFilter`      | Extracts the bearer token, parses it, binds the tenant, verifies the user is still active with the same roles, sets the `Authentication`, and binds the log context. Always clears tenant and MDC keys in `finally`. |
| `security.jwt.JwtUtil`          | Generates staff tokens (24 h) and guest tokens (short lived, subject `0`), parses and verifies tokens, extracts roles. Parsing never throws: an invalid, expired or tampered token simply yields no claims.          |
| `security.jwt.JwtClaims`        | Constants for every claim name (role, roles, clinic id, name, slug, username). Never use claim name literals elsewhere.                                                                                              |
| `security.ActiveUserChecker`    | Rejects tokens of deactivated users or whose role set changed since the token was issued (guests always pass). Used by both the HTTP filter and the STOMP interceptor.                                               |
| `security.RoleExpressions`      | SpEL constants for `@PreAuthorize` (`STAFF`, `ADMINISTRATOR`, `ADMINISTRATOR_OR_ASSISTANT`, `DOCTOR`, `GUEST`) and the `STAFF_ROLES` list used by the filter chain.                                                  |
| `security.SecurityUtils`        | Static helpers: bearer extraction, building the authenticated token with `ROLE_` authorities, role checks, current user id, and `isRestrictedToOwnData` (doctor that is not also administrator).                     |
| `config.PasswordEncoderConfig`  | BCrypt `PasswordEncoder` bean.                                                                                                                                                                                       |
| `config.WebConfig`              | CORS: origins from `cors.allowed-origins`, allowed methods, exposed headers (request id, `Retry-After`, rate limit remaining), no credentials.                                                                       |
| `exception.ErrorResponseWriter` | Writes the standard localized `ErrorResponse` for 401 (`security.authenticationRequired`) and 403 (`security.accessDenied`) from the chain handlers.                                                                 |

## Roles and tokens

- Roles come from `ERole`: `ADMINISTRATOR`, `DOCTOR`, `ASSISTANT` (staff) and `GUEST`. An administrator can also be a doctor; then the token carries both roles and the `role` claim holds the primary one.
- Authorities are always `ROLE_` plus the role name, built only through `SecurityUtils`.
- The principal name is the user id as a string; read it with `SecurityUtils.extractUserId`, never by parsing the token again.
- Every token carries the clinic id and slug; the filter binds the tenant from them, so the tenant can never be chosen by the client through headers or parameters.
- Guest tokens are issued by the public guest session endpoint, expire quickly and only grant access to `/api/guest/**`. Guests cannot open websocket connections.
- The secret is `jwt.secret` (`JWT_SECRET`, at least 32 bytes for HS256). It is never hardcoded, logged or returned.

## Request flow

1. CORS preflight is answered by the CORS configuration.
2. `JwtAuthFilter` runs before `UsernamePasswordAuthenticationFilter`. Missing or invalid tokens leave the request anonymous; the filter never writes a response itself. Rejections (inactive user, changed role, lookup failure, bad claims) are logged as warnings with the reason, never with the token.
3. `RateLimitFilter` runs right after the JWT filter so it can key by user (see `spring-rate-limiting`). Its servlet auto registration is disabled so it runs only once, inside the chain.
4. Path rules, in order: `security.public-paths` are permitted, `/actuator/**` is denied (only health is public), the error dispatcher is permitted, `/api/guest/**` requires `GUEST`, everything else requires a staff role.
5. Method security (`@PreAuthorize` with `RoleExpressions`) narrows the role per endpoint.
6. Authentication failures return 401 and authorization failures 403, both as the localized JSON error contract with the trace id.

## Rules

- Every controller endpoint has an explicit `@PreAuthorize` using a `RoleExpressions` constant, even if the path rule already covers it. Do not write SpEL strings inline; add a constant if a new combination is needed.
- Public endpoints are declared only in `security.public-paths` in `application.yaml`. Making a path public also requires an IP rate limit policy (see `spring-rate-limiting`) and, if it is not an API path, adding it to `security.system-paths` when it should not be access logged.
- Data scoping by user belongs in the service: the controller passes the current user id and `isRestrictedToOwnData` and the service filters or throws. Never trust ids in the body or path to decide ownership.
- Resource-level denials raised from services use the exception hierarchy (`spring-exceptions`), not ad hoc responses.
- Passwords are hashed with the `PasswordEncoder` bean only. Login failures return the same generic message for unknown clinic, unknown user and wrong password to avoid user enumeration; the specific reason goes only to the log.
- Keep the API stateless: no `HttpSession`, no `SecurityContext` persistence, no cookies with credentials. If cookies are ever introduced, CSRF must be re-enabled for them.
- Code that runs outside a request (async listeners, schedulers, websocket) must not rely on `SecurityContextHolder`; pass the user id explicitly.
- Websocket authentication is done at STOMP level by `JwtChannelInterceptor`, reusing `JwtUtil`, `ActiveUserChecker` and `SecurityUtils` (see `spring-websocket`). Do not secure the HTTP handshake separately.
- New CORS exposed headers go in `WebConfig`; never use wildcard origins in production environments.
- New user-facing security messages are `MessageKeys` constants present in both bundles (`spring-i18n`).

## Adding or protecting an endpoint (checklist)

1. Decide the audience: staff roles, guest, or public.
2. Staff or guest: add `@PreAuthorize` with the right `RoleExpressions` constant and, if the endpoint deals with doctor-owned data, pass the user id and the restriction flag to the service.
3. Public: add the path to `security.public-paths`, add a rate limit policy, and make sure the response never leaks whether an account exists.
4. Adding a role or claim: update `ERole` or `JwtClaims`, `JwtUtil` generation and extraction, `ActiveUserChecker`, `RoleExpressions`, the filter chain rules and the websocket destination rules together.
5. Test: MockMvc slice tests for 401 without token, 403 with a wrong role and success with the right role; unit tests for `JwtUtil` (expired, tampered, missing roles claim) and `ActiveUserChecker` when they change.
