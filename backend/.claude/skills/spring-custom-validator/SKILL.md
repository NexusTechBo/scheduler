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
            return value == ERole.ASSISTANT || value == ERole.DOCTOR;   // ADMINISTRATOR cannot be assigned via the API
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
// @AllowedValues({"DOCTOR", "ASSISTANT"}) ERole role
```

## 2. Class validator (rule across fields)

```java
@Documented
@Constraint(validatedBy = SpecialtyRoleMatch.SpecialtyRoleMatchValidator.class)
@Target({ ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
public @interface SpecialtyRoleMatch {
    String message() default "";
    String first();                       // name of the role field
    String second();                      // name of the dependent field
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};

    @Target({ ElementType.TYPE })
    @Retention(RetentionPolicy.RUNTIME)
    @Documented
    @interface List { SpecialtyRoleMatch[] value(); }    // allows repeating the annotation

    class SpecialtyRoleMatchValidator implements ConstraintValidator<SpecialtyRoleMatch, Object> {
        private String firstField;
        private String secondField;

        @Override
        public void initialize(SpecialtyRoleMatch a) {
            this.firstField = a.first();
            this.secondField = a.second();
        }

        @Override
        public boolean isValid(Object value, ConstraintValidatorContext context) {
            if (value == null) return true;
            try {
                var role = (ERole) getFieldValue(value, firstField);
                var specialtyId = (Long) getFieldValue(value, secondField);
                if (role == null) return true;
                if (role == ERole.DOCTOR && specialtyId == null) {
                    buildViolation(context, "The doctor role should have a specialty", secondField);
                    return false;
                }
                if (role == ERole.ASSISTANT && specialtyId != null) {
                    buildViolation(context, "Only doctors can have a specialty", secondField);
                    return false;
                }
                return true;
            } catch (Exception e) {
                return false;
            }
        }

        // Error attached to a specific field (shows up as `field` in the response, not as a global object error)
        private void buildViolation(ConstraintValidatorContext context, String message, String node) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(message)
                    .addPropertyNode(node)
                    .addConstraintViolation();
        }

        // Supports records (accessor name()) and POJOs (getName())
        private Object getFieldValue(Object object, String fieldName) throws Exception {
            var clazz = object.getClass();
            try {
                return clazz.getMethod(fieldName).invoke(object);
            } catch (NoSuchMethodException ignored) {}
            var getter = "get" + fieldName.substring(0, 1).toUpperCase() + fieldName.substring(1);
            return clazz.getMethod(getter).invoke(object);
        }
    }
}
```
Usage:
```java
@SpecialtyRoleMatch(first = "role", second = "specialtyId")
public record UserRequest(@NotNull @ValidRole ERole role, Long specialtyId, ...) {}
```

For a single DTO it is simpler and reflection-free to implement a typed validator `ConstraintValidator<Annotation, UserRequest>` and read `value.role()` directly. Use the reflective version only when the rule is reused across several DTOs (e.g. create and update).

Other typical cases with the same pattern: `@DateRange(start = "from", end = "to")` (start < end), `@FieldsMatch(first = "password", second = "confirmPassword")`, `@ConditionalRequired(when = "fullDay", equals = "false", required = {"startTime", "endTime"})`.

## 3. Messages with placeholders

```java
String message() default "must be one of {value}";
```
Or externalize to `ValidationMessages.properties` with `message() default "{validation.role.notAllowed}"`.

## 4. Minimal test

```java
var validator = Validation.buildDefaultValidatorFactory().getValidator();
var violations = validator.validate(new UserRequest(ERole.DOCTOR, null, ...));
assertThat(violations).extracting(v -> v.getPropertyPath().toString()).containsExactly("specialtyId");
```
