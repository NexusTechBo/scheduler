---
name: spring-security-jwt
description: 'Stateless JWT security with Spring Security 6/7: SecurityFilterChain, JWT filter (OncePerRequestFilter), token generation/parsing with jjwt, roles with @PreAuthorize, active-user check on every request, configurable public paths and JSON 401/403 responses. Use when adding authentication, login, roles, protecting endpoints or debugging 401/403.'
---

# Stateless JWT security

Flow: `POST /api/auth/login` validates credentials → returns a JWT (`sub` = userId, claims `role`, `tenantId`, `username`). Every request carries `Authorization: Bearer <token>`; `JwtAuthFilter` validates it, **checks in the DB that the user is still active and has the same role**, and fills the `SecurityContext` with `ROLE_<role>`. Fine-grained authorization with `@PreAuthorize` on controllers.

## Dependencies

```groovy
implementation 'org.springframework.boot:spring-boot-starter-security'
implementation 'io.jsonwebtoken:jjwt-api:0.12.6'
runtimeOnly    'io.jsonwebtoken:jjwt-impl:0.12.6'
runtimeOnly    'io.jsonwebtoken:jjwt-jackson:0.12.6'
```

## 1. Roles

```java
public enum ERole { ADMINISTRATOR, DOCTOR, ASSISTANT }
```
Stored on the entity with `@Enumerated(EnumType.STRING)` + `CHECK (role IN (...))` in the migration.

## 2. JwtUtil

```java
@Component
public class JwtUtil {
    private final SecretKey key;

    public JwtUtil(@Value("${jwt.secret}") String secret) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));   // >= 32 bytes for HS256
    }

    public String generate(Long userId, String role, Long tenantId, String userName) {
        return Jwts.builder()
                .subject(userId.toString())
                .claim("role", role)
                .claim("tenantId", tenantId)          // omit without multi-tenancy
                .claim("username", userName)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 86_400_000L))   // 24h
                .signWith(key)
                .compact();
    }

    public Optional<Claims> parse(String token) {
        try {
            return Optional.of(Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload());
        } catch (JwtException | IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}
```

## 3. ActiveUserChecker (implicit revocation)

```java
@Component
@RequiredArgsConstructor
public class ActiveUserChecker {
    private final UserRepository userRepository;

    /** true if the user is still active with the token's role; a token issued before a role change stops working. */
    public boolean isActive(Long userId, String role) {
        return Arrays.stream(ERole.values())
                .filter(r -> r.name().equals(role))
                .findFirst()
                .map(r -> userRepository.existsByIdAndActiveTrueAndRole(userId, r))
                .orElse(false);
    }
}
```
Costs one `SELECT 1` per request; in exchange, deactivating a user or changing their role takes effect immediately with no token blacklist.

## 4. JwtAuthFilter

```java
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {
    private final JwtUtil jwtUtil;
    private final ActiveUserChecker activeUserChecker;

    @Override
    protected void doFilterInternal(HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull FilterChain chain)
            throws ServletException, IOException {
        try {
            var header = request.getHeader("Authorization");
            if (header != null && header.startsWith("Bearer ")) {
                jwtUtil.parse(header.substring(7)).ifPresent(claims -> {
                    TenantContext.setCurrentTenant("tenant_" + claims.get("tenantId", Long.class)); // multi-tenancy only, BEFORE touching the DB
                    var role = claims.get("role", String.class);
                    var userId = Long.valueOf(claims.getSubject());
                    try {
                        if (!activeUserChecker.isActive(userId, role)) return;
                    } catch (DataAccessException e) {
                        log.warn("Active user lookup failed for user {}: {}", userId, e.getMessage());
                        return;                                       // fail closed: stays anonymous → 401
                    }
                    var authorities = List.of(new SimpleGrantedAuthority("ROLE_" + role));
                    SecurityContextHolder.getContext().setAuthentication(
                            new UsernamePasswordAuthenticationToken(claims.getSubject(), null, authorities));
                });
            }
            chain.doFilter(request, response);
        } finally {
            TenantContext.clear();                                    // ThreadLocal: always clear
        }
    }
}
```
On HTTP the filter never responds by itself and `ifPresent` is intentional: an invalid/expired token leaves the request anonymous, and the `authenticationEntryPoint` answers 401 only if the route requires authentication (public routes keep working with a stale token). This differs from the WebSocket CONNECT, which must reject invalid tokens (see `spring-websocket`).

## 5. SecurityConfig

```java
@Configuration
@EnableWebSecurity
@EnableMethodSecurity                       // enables @PreAuthorize
@RequiredArgsConstructor
public class SecurityConfig {
    private final JwtAuthFilter jwtAuthFilter;
    private final ObjectMapper objectMapper;  // SB4: tools.jackson.databind.ObjectMapper

    @Value("${security.public-paths:}")
    private String[] publicPaths;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) {   // SB3: throws Exception
        return http
                .csrf(AbstractHttpConfigurer::disable)             // stateless + bearer: no CSRF
                .cors(Customizer.withDefaults())                   // uses WebConfig.addCorsMappings
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(a -> {
                    if (publicPaths.length > 0) a.requestMatchers(publicPaths).permitAll();
                    a.requestMatchers("/ws/**").permitAll();       // WS handshake; auth happens on STOMP CONNECT
                    a.anyRequest().authenticated();
                })
                .exceptionHandling(e -> e
                        .authenticationEntryPoint((req, res, _) -> writeError(req, res, HttpStatus.UNAUTHORIZED, "Authentication required"))
                        .accessDeniedHandler((req, res, _) -> writeError(req, res, HttpStatus.FORBIDDEN, "Access denied")))
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .build();
    }

    private void writeError(HttpServletRequest request, HttpServletResponse response, HttpStatus status, String message) throws IOException {
        var body = ErrorResponse.of(status, message, request.getRequestURI());
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        objectMapper.writeValue(response.getWriter(), body);
    }
}
```
Public paths in yaml: `security.public-paths: /api/auth/login, /swagger-ui/**, /v3/api-docs/**`.
`PasswordEncoder` lives in its own `@Configuration` (see `spring-essential-config`).

## 6. SecurityUtils

```java
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class SecurityUtils {
    public static String extractRole(Authentication auth) {
        return auth.getAuthorities().stream()
                .map(a -> Objects.requireNonNull(a.getAuthority()).replace("ROLE_", ""))
                .findFirst().orElse(null);
    }
}
```
In controllers: `Long.parseLong(auth.getName())` = userId.

## 7. Login (AuthService)

```java
public LoginResponse login(LoginRequest request) {
    var user = userRepository.findByUsername(request.username().toLowerCase(Locale.ROOT))
            .orElseThrow(() -> new UnauthorizedException("Invalid Credentials"));
    if (!user.isActive()) throw new UnauthorizedException("Your user is not active");
    if (!passwordEncoder.matches(request.password(), user.getPassword()))
        throw new UnauthorizedException("Invalid Credentials");                // same message: don't reveal what failed
    return new LoginResponse(jwtUtil.generate(user.getId(), user.getRole().name(), tenantId, user.getName()));
}
```
With multi-tenancy the username carries the tenant (`john@acme.com` → the `@acme.com` suffix identifies the tenant in `public`); set `TenantContext` and look the user up inside that schema (see `spring-multi-tenancy`).

## 8. Authorization

- Coarse: `@PreAuthorize("hasAnyRole('ADMINISTRATOR', 'ASSISTANT')")` per endpoint.
- Ownership (resource owner): in the service with `userId`/`role`, throwing `ForbiddenException`, in a `private` helper with a null-safe comparison:
  ```java
  private void verifyOwnership(String role, Long ownerId, Long userId) {
      if (ERole.DOCTOR.name().equals(role) && !ownerId.equals(userId))
          throw new ForbiddenException("Not authorized to do this");
  }
  ```
- Changing your own password: require `currentPassword` and check it with `passwordEncoder.matches`.

## Checklist

- [ ] `JWT_SECRET` >= 32 characters, only via env var.
- [ ] `open-in-view: false` and `STATELESS` sessions.
- [ ] Never log tokens or passwords.
- [ ] Every new public route added to `security.public-paths`, not scattered `permitAll` calls.
