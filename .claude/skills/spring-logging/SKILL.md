---
name: spring-logging
description: Logging conventions in the scheduler backend (SLF4J with Lombok, structured ECS logs, MDC context with trace, clinic and user, access log, masking of personal data). Use when adding log statements, choosing a log level, logging in async or scheduled code, adding a context key, tuning log configuration, or debugging a request by its trace id.
---

# Spring Logging

Logging is configured through Spring Boot properties only (no `logback.xml`). Every log line automatically carries the request context through the MDC, and every request produces exactly one access log line.

## Building blocks

| Piece                             | Role                                                                                                                                                                                                         |
|-----------------------------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `logging.*` in `application.yaml` | Structured console output (`LOG_FORMAT`, ECS by default) and the application level (`LOG_LEVEL`, INFO by default). The `dev` profile switches to plain text and enables SQL and bind parameter logging.      |
| Micrometer Tracing (Brave)        | Creates the trace and puts `traceId` in the MDC for every request.                                                                                                                                           |
| `util.LogKeys`                    | Constants for MDC keys (`traceId`, `clinicId`, `clinicSlug`, `userId`, `role`), structured field names (`method`, `path`), the `X-Request-Id` header, the guest user marker and the request attribute names. |
| `util.LogUtils`                   | Helpers for safe values: email masking, sanitizing free text (masks emails, removes line breaks), truncating and cleaning usernames, and a dash for missing values.                                          |
| `middleware.AccessLogFilter`      | Runs early, returns the trace id in `X-Request-Id`, and logs one line per request with method, path, status, duration, clinic, user and role. Skips system paths, static resources and preflight requests.   |
| `middleware.JwtAuthFilter`        | Puts `userId` and `role` in the MDC (guests as `guest`) and copies clinic, user and role to request attributes so the access log can still read them after the MDC is cleaned.                               |
| `tenant.TenantContext`            | Puts and removes `clinicId` and `clinicSlug` in the MDC whenever the tenant is bound, restored or cleared.                                                                                                   |
| `config.MdcTaskDecorator`         | Copies the caller's MDC into async executor threads and restores the previous one afterwards. Set on every `ThreadPoolTaskExecutor` in `AsyncConfig`.                                                        |
| `exception.ErrorResponse`         | Includes the `traceId` in every error body so a user report can be matched to the logs.                                                                                                                      |

## Levels

| Level   | Use for                                                                                                                                                                                  |
|---------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `ERROR` | Unexpected failures that need attention: unhandled exceptions (with the stack trace, logged once in `GlobalExceptionHandler`), dropped tasks, 5xx.                                       |
| `WARN`  | Expected but notable problems: business errors, rejected tokens, failed logins, rate limit hits, integrity or optimistic locking conflicts, failed notifications, 4xx in the access log. |
| `INFO`  | Relevant business events: login succeeded, clinic activated, bulk cancellations, skipped notifications, successful 2xx/3xx in the access log.                                            |
| `DEBUG` | Diagnostic detail useful only while investigating; must be cheap and safe to enable in production.                                                                                       |

## Rules

- Declare loggers with Lombok `@Slf4j`; never create loggers manually or use `System.out`/`printStackTrace`.
- Use placeholders, never string concatenation. Use the fluent API with `addKeyValue` when a field should be queryable in structured output (as the access log, rate limit and login failure logs do), and keep a readable message as well.
- Field names for context values come from `LogKeys`; do not invent new spellings of clinic, user or role.
- Do not log what is already in the MDC (trace, clinic, user) unless the line is about a different entity, for example the target user of an admin action.
- Log an exception once, where it is handled. Services throw; `GlobalExceptionHandler` logs. Expected exceptions are logged without stack trace (class name or message key is enough); unexpected ones with the exception as last argument.
- Never log secrets or credentials: passwords, tokens, the JWT secret, API keys, activation codes, `Authorization` headers.
- Never log personal data in clear: mask emails with `LogUtils.maskEmail`, sanitize free text from users or external services with `sanitizeForLog`, clean usernames with `sanitizeUsername`. Do not log phone numbers, patient names, request or response bodies. Prefer ids.
- Sanitize any user-controlled value before logging to prevent log injection (line breaks, control characters, unbounded length).
- New MDC keys need a constant in `LogKeys`, must be removed in the same `finally` that sets them, and must be propagated by `MdcTaskDecorator` (automatic if it is in the MDC when the task is submitted).
- Every new async executor sets `MdcTaskDecorator`. Schedulers and listeners that bind a tenant use `TenantContext`, which keeps the MDC in sync; never put clinic keys in the MDC directly.
- Log messages are in English and are not localized; only user-facing messages go through the message bundles.
- Do not add per-request logging in controllers or services; the access log already covers it. Do not log inside tight loops.
- Change verbosity through `LOG_LEVEL` or `logging.level.*` properties, not by editing code. SQL and bind logging belong only to the `dev` profile.

## Adding logging to a feature (checklist)

1. Add `@Slf4j` and log only business events (INFO) and handled abnormal situations (WARN) that are not already covered by the access log or the exception handler.
2. Use ids and `LogKeys` field names; mask or sanitize anything personal or user-controlled.
3. If the work runs async, confirm the executor uses `MdcTaskDecorator` and the tenant is bound through `TenantContext`.
4. If a new environment variable controls logging, add it to `application.yaml` with a default and to `.env.example`.
5. Test: for filters or helpers, unit test `LogUtils` behaviour and that MDC keys are cleared after the chain, including when it throws.
