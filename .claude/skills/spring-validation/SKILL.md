---
name: spring-validation
description: Request validation in the scheduler backend with Jakarta Bean Validation and custom constraints. Use when adding validation to request records, creating a custom constraint (field or cross-field), validating path or query parameters, or deciding whether a rule belongs to validation or to the service.
---

# Spring Validation

Input is validated at the edge with Bean Validation on request records. Business rules that need the database or the current user belong to the service and are reported with exceptions.

## Where each rule goes

| Rule type                                                                                               | Place                                                                                         |
|---------------------------------------------------------------------------------------------------------|-----------------------------------------------------------------------------------------------|
| Shape of a single field (required, length, format, range)                                               | Standard constraint on the record component.                                                  |
| Field rule reused in several requests                                                                   | Custom field-level constraint in `validator.<name>`.                                          |
| Relationship between fields of the same request (time range order, role requires specialty)             | Custom class-level constraint plus a small interface that the request record implements.      |
| Needs the database, the clock in a business sense, or the logged user (uniqueness, ownership, overlaps) | Service, throwing an `ApiException`.                                                          |
| Path variables and query parameters                                                                     | Constraints on the controller parameters; method validation is handled by the global handler. |
| Configuration properties                                                                                | `@Validated` on the properties record so the app fails fast on startup.                       |

## Custom constraint conventions

- One folder per constraint inside `validator`, named after the constraint in camel case.
- The annotation and its validator live in the same file: the validator is a nested class of the annotation. This keeps the constraint self-contained.
- Class-level constraints declare a nested `Request` interface with the accessor methods they need. Request records implement that interface, so one constraint works for several records without reflection.
- The default message references a bundle key in curly braces (`validation.<constraint>.<reason>`). Add the text to both message bundles.
- Validators return true for null input and let `@NotNull` handle presence, so constraints compose.
- Cross-field violations disable the default violation and attach the message to the specific property node, so the frontend can highlight the right field.
- Validators are stateless and do not inject repositories or services.
- Keep the existing constraints in mind before writing a new one: maximum UTF-8 bytes (for bcrypt-safe passwords), partial block times, valid staff role, specialty and role match, time range.

## Controller usage

- Use `@Valid` on every `@RequestBody`.
- Strings are stripped of surrounding whitespace globally by the custom Jackson deserializer, so validators see trimmed values.
- Size limits must match the database column length.
- Validation errors are returned as status 400 with a localized summary message and a list of field errors (field name plus message). Do not catch validation exceptions in controllers.

## Testing

- Unit test each custom validator directly with valid, invalid and null input, and assert the property node of the violation for cross-field rules.
- Controller slice tests should cover at least one invalid body per endpoint to assert the 400 contract.
