---
name: spring-exceptions
description: Exception hierarchy and centralized error handling in the scheduler backend. Use when throwing errors from services, adding a new error type or HTTP status, handling a new framework exception, or changing the error response contract consumed by the frontend.
---

# Spring Exceptions

Errors are thrown as typed exceptions that carry an HTTP status and a message key. A single `@RestControllerAdvice` turns them into a stable, localized JSON contract.

## Hierarchy

`ApiException` is the abstract base (unchecked) holding status, message key and arguments. Concrete subclasses map to one status each:

| Exception                     | Status | Use for                                                                                                           |
|-------------------------------|--------|-------------------------------------------------------------------------------------------------------------------|
| `BadRequestException`         | 400    | Input that is syntactically valid but semantically wrong and cannot be expressed as a Bean Validation constraint. |
| `UnauthorizedException`       | 401    | Invalid credentials or unusable token.                                                                            |
| `ForbiddenException`          | 403    | Authenticated user acting on a resource they do not own or a role that cannot perform the action.                 |
| `ResourceNotFoundException`   | 404    | Entity not found by id or natural key in the current tenant.                                                      |
| `BusinessException`           | 409    | Business rule conflicts: duplicates, overlapping schedules, invalid state transitions.                            |
| `ServiceUnavailableException` | 503    | External dependency down (WhatsApp service, mail, AI).                                                            |

Add a new subclass only for a status not covered above; never create one exception class per business case. The business case is expressed by the message key.

## Error contract

`ErrorResponse` is a record with timestamp, status, reason phrase, localized message, path, optional field errors and the trace id from the MDC. Null fields are omitted. The frontend reads `message` and `errors[].message`, so changes to this record are breaking changes.

## Global handler responsibilities

- `ApiException`: log at warn with status, key, method and path; resolve the key with arguments for the current locale.
- Validation (body, method parameters, constraint violations): 400 with the list of field errors.
- Malformed body, type mismatch, missing parameter, invalid sort property: 400 with a specific key.
- Unknown route, method not supported, unsupported media type: 404, 405, 415.
- Data integrity violation: 409, logging only the constraint name, never the SQL or values.
- Optimistic locking failure: 409 with a retry message.
- Access denied: 403.
- Anything else: 500 with a generic message, logged at error with the stack trace.

Filters run outside the dispatcher, so they cannot rely on the advice. They use `ErrorResponseWriter`, which produces the same contract (used by the rate limit filter and the security entry points).

## Rules

- Services throw; controllers never catch. No try/catch whose only purpose is to translate an exception into a response.
- Always use a `MessageKeys` constant and pass arguments instead of concatenating text. See the `spring-i18n` skill.
- Do not leak internals: no stack traces, SQL, class names or entity contents in messages.
- Do not use exceptions for normal control flow; prefer `Optional` and `exists` checks, then throw at the decision point.
- Prefer `orElseThrow` with the specific exception right after the repository call.
- Catch checked or third-party exceptions at the integration boundary (exchange clients, mail) and rethrow as the appropriate `ApiException`, keeping the original as the cause for logs.
- Async listeners and websocket publishers log and swallow, because there is no client waiting for a response.

## Testing

- Service unit tests assert the exception type and the message key.
- Controller slice tests assert the status code and the JSON shape of the error response for the main failure paths.
