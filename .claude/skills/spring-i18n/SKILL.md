---
name: spring-i18n
description: Internationalization of backend messages in the scheduler project (responses fixed to Spanish; both bundles maintained). Use when adding error or validation messages, new message keys, parameterized or plural messages, or changing how the locale is resolved.
---

# Spring i18n

All user-facing text produced by the backend (business errors, validation errors, rate limit messages, notification text) is resolved from message bundles according to the request locale.

## Building blocks

| Piece                                              | Role                                                                                                                                                                   |
|----------------------------------------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `config.LocaleConfig`                              | Fixed Spanish locale resolver (`FixedLocaleResolver`, `Accept-Language` is ignored), the `MessageSource` bean, and the validator factory wired to the same bundles.    |
| `messages.properties`                              | English texts (base bundle).                                                                                                                                           |
| `messages_es.properties`                           | Spanish texts.                                                                                                                                                         |
| `exception.MessageKeys`                            | Constant holder with every key used from Java code.                                                                                                                    |
| `GlobalExceptionHandler` and `ErrorResponseWriter` | Resolve keys plus arguments into the final message for the current locale.                                                                                             |

Responses are always in Spanish: the frontend does not send `Accept-Language` and the resolver ignores it. The English bundle is still maintained so a language switch can be reintroduced by changing only the resolver.

## Rules

- Never hardcode a user-facing string in Java. Use a key.
- Every key must exist in both bundles. Adding a key to only one file is a bug.
- Keys used from Java code are declared once in `MessageKeys`, grouped by feature, named `<feature>.<camelCaseReason>`. Keys used only by validation annotations are referenced from the annotation message attribute with curly braces and do not need a constant.
- Exceptions carry the key and its arguments, not the resolved message. Resolution happens only at the edge (handler or writer).
- Use `MessageFormat` placeholders for arguments. Remember that a literal single quote must be doubled in a message that has arguments.
- Use the `choice` format for singular and plural variants instead of building strings in code.
- Bundles are UTF-8 and fallback to the system locale is disabled, so an unknown locale falls back to the base bundle predictably.
- The message source uses the code as the default message, so a missing key shows up as the raw key: treat any raw key in a response as a missing translation.
- Validation messages for custom constraints live in the same bundles, under `validation.<constraint>.<reason>`.
- Logs are in English and are not translated.

## Adding a message (checklist)

1. Add the constant to `MessageKeys` in the right feature group (if used from Java).
2. Add the English text to `messages.properties` and the Spanish text to `messages_es.properties`, keeping the same ordering and grouping in both files.
3. Throw the exception or reference the key from the annotation.
4. Test: a controller slice test can assert the resolved Spanish message.
