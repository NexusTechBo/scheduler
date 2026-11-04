---
name: spring-rate-limiting
description: Configuration-driven rate limiting in the scheduler backend with Bucket4j and Caffeine. Use when protecting a new endpoint, tuning limits, changing how clients are identified, or debugging HTTP 429 responses.
---

# Spring Rate Limiting

Rate limiting is implemented once, in a servlet filter, and driven entirely by configuration. Adding protection to an endpoint should normally require only a YAML change.

## Building blocks

| Class                               | Role                                                                                                                                     |
|-------------------------------------|------------------------------------------------------------------------------------------------------------------------------------------|
| `ratelimiting.RateLimitProperties`  | Validated `@ConfigurationProperties` record bound to the `rate-limit` prefix: enabled flag, bucket cache size, excluded paths, policies. |
| `ratelimiting.RateLimitPolicy`      | One named policy: optional HTTP methods, path patterns, key strategy and one or more bandwidths.                                         |
| `ratelimiting.RateLimitBandwidth`   | Capacity and period of a single limit.                                                                                                   |
| `ratelimiting.RateLimitKeyStrategy` | How a client is identified: `IP` or `USER`.                                                                                              |
| `middleware.RateLimitFilter`        | Compiles the policies at startup, resolves the key, consumes a token and writes the 429 response.                                        |

## How the filter decides

1. Skips everything when disabled and always skips CORS preflight requests.
2. Skips paths in `excluded-paths` (actuator, Swagger, websocket, favicon).
3. Picks the first policy whose methods and path patterns match. Order in YAML matters: put specific policies before broad ones.
4. Resolves the bucket key: `USER` uses clinic id plus user id from request attributes set by the JWT filter, falling back to IP for guests or anonymous calls; `IP` uses the remote address, collapsing IPv6 to its /64 prefix so a single client cannot rotate addresses.
5. Consumes one token. On success it adds the `X-RateLimit-Remaining` header. On failure it sets `Retry-After`, logs a structured warning and writes a localized `ErrorResponse` with status 429, using a seconds message for short waits and a minutes message for long ones.

Buckets live in a bounded Caffeine cache that expires entries after the longest period of the policy, so memory stays bounded.

## Rules

- Add or tune limits only in `application.yaml` under `rate-limit.policies`, with every number exposed as an environment variable with a sensible default.
- Combine a short burst limit with a longer window limit for sensitive endpoints (login, registration, activation resend).
- Use `IP` for public or unauthenticated endpoints and `USER` for authenticated, expensive endpoints.
- The filter must run after the JWT filter so `USER` keys can be resolved; keep that ordering in the security configuration.
- Any new header the frontend needs to read must be listed in the exposed headers of the CORS config.
- Error messages come from `MessageKeys` and both message bundles; never hardcode text in the filter.
- The in-memory buckets are per instance. If the app is ever scaled horizontally, move the buckets to a distributed Bucket4j backend instead of adding custom logic.
- Do not add per-controller or annotation-based throttling; keep one mechanism.

## Adding a policy (checklist)

1. Add a named policy with methods, paths, key strategy and limits in `application.yaml`.
2. Add the matching environment variables to `.env.example`.
3. Check the policy does not get shadowed by an earlier broader policy.
4. Test: unit test the filter with a small properties object and a mocked `ErrorResponseWriter`; assert headers, status and that the chain is or is not invoked.
