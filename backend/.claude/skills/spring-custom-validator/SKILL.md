---
name: spring-custom-validator
description: 'Create custom Jakarta Bean Validation validators in Spring Boot: field-level annotation (allowed value) and class-level annotation (rule across two fields of a record/DTO) with a nested ConstraintValidator, per-field messages and null handling. Use when a validation rule cannot be expressed with @NotNull/@Size/@Pattern or depends on several fields.'
---

# Custom validators

Convention: one package per validator `validator/<camelName>/`, a single file with the `@interface` and the `ConstraintValidator` as a nested class. Errors surface via `MethodArgumentNotValidException` → `GlobalExceptionHandler` → 400 `errors[{field, message}]`.

Rules:
- `null` → `return true`. Required-ness is `@NotNull`'s job; that way they can be combined.
- No DB access inside the validator (uniqueness/existence belongs in the service, with the right tenant and transaction).
- Messages in the API's language, consistent with the rest.

## 1. Field validator (allowed value)

```java
@Documented
@Constraint(validatedBy = ValidRole.ValidRoleValidator.class)
@Target({ ElementType.FIELD, ElementType.PARAMETER })
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidRole {
    String message() default "The role you have entered is not permitted";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};

    class ValidRoleValidator implements ConstraintValidator<ValidRole, ERole> {
        @Override
        public boolean isValid(ERole value, ConstraintValidatorContext context) {
            if (value == null) return true;
            return value == ERole.STAFF || value == ERole.MEMBER;   // ADMINISTRATOR cannot be assigned via the API
        }
    }
}
```
Usage:
```java
public record UserRequest(@NotNull @ValidRole ERole role, ...) {}
```

Parameterizable variant (avoids one validator per enum):
```java
public @interface AllowedValues {
    String[] value();
    String message() default "Value not allowed";
    ...
    class Validator implements ConstraintValidator<AllowedValues, Enum<?>> {
        private Set<String> allowed;
        @Override public void initialize(AllowedValues a) { allowed = Set.of(a.value()); }
        @Override public boolean isValid(Enum<?> v, ConstraintValidatorContext c) { return v == null || allowed.contains(v.name()); }
    }
}
// @AllowedValues({"MEMBER", "STAFF"}) ERole role
```

## 2. Class validator (rule across fields)

Typed, no reflection: the validator receives the request itself. When the rule is used by several DTOs (e.g. create and update), the annotation declares a small nested `Request` interface with the accessors it needs; records implement it for free.

```java
@Documented
@Constraint(validatedBy = TeamRoleMatch.TeamRoleMatchValidator.class)
@Target({ ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
public @interface TeamRoleMatch {
    String message() default "Invalid team for the given role";   // never empty
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};

    // Implemented by the requests annotated with @TeamRoleMatch; record accessors satisfy it
    interface Request {
        ERole role();
        Long teamId();
    }

    class TeamRoleMatchValidator implements ConstraintValidator<TeamRoleMatch, Request> {
        @Override
        public boolean isValid(Request value, ConstraintValidatorContext context) {
            if (value == null || value.role() == null) return true;
            var existsTeam = value.teamId() != null;
            if (value.role() == ERole.MEMBER && !existsTeam) {
                buildViolation(context, "The member role should have a team");
                return false;
            }
            if (value.role() == ERole.STAFF && existsTeam) {
                buildViolation(context, "Only members can have a team");
                return false;
            }
            return true;
        }

        // Error attached to a specific field (shows up as `field` in the response, not as a global object error)
        private void buildViolation(ConstraintValidatorContext context, String errorMessage) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(errorMessage)
                    .addPropertyNode("teamId")
                    .addConstraintViolation();
        }
    }
}
```
Usage:
```java
@TeamRoleMatch
public record UserRequest(@NotNull @ValidRole ERole role, Long teamId, ...) implements TeamRoleMatch.Request {}
```

Rule used by a single DTO: skip the interface and type the validator against the DTO directly, `ConstraintValidator<Annotation, MyRequest>`, and read `value.field()` directly.

Don't use reflection with field names as strings (`@X(first = "role", second = "teamId")`): a typo still compiles, and a `catch -> return false` turns it into a silent 400 with an empty message. The interface makes it a compile-time contract.

Other typical cases with the same pattern: `@DateRange` (start before end), `@FieldsMatch` (password/confirmPassword), conditional required fields (a flag that makes other fields mandatory).

## 3. Messages with placeholders

```java
String message() default "must be one of {value}";
```
Or externalize to `ValidationMessages.properties` with `message() default "{validation.role.notAllowed}"`.

## 4. Minimal test

```java
var validator = Validation.buildDefaultValidatorFactory().getValidator();
var violations = validator.validate(new UserRequest(ERole.MEMBER, null, ...));
assertThat(violations).extracting(v -> v.getPropertyPath().toString()).containsExactly("teamId");
```
