package com.example.scheduler.validator.maxUtf8Bytes;

import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;
import java.lang.annotation.*;
import java.nio.charset.StandardCharsets;

@Documented
@Constraint(validatedBy = MaxUtf8Bytes.MaxUtf8BytesValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface MaxUtf8Bytes {
    String message() default "{validation.maxUtf8Bytes}";

    int max() default 72;

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    class MaxUtf8BytesValidator implements ConstraintValidator<MaxUtf8Bytes, CharSequence> {
        private int max;

        @Override
        public void initialize(MaxUtf8Bytes annotation) {
            this.max = annotation.max();
        }

        @Override
        public boolean isValid(CharSequence value, ConstraintValidatorContext context) {
            if (value == null) return true;
            return value.toString().getBytes(StandardCharsets.UTF_8).length <= max;
        }
    }
}
