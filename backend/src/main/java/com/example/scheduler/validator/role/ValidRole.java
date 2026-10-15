package com.example.scheduler.validator.role;

import com.example.scheduler.enums.ERole;
import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = ValidRole.ValidRoleValidator.class)
@Target({ ElementType.FIELD, ElementType.PARAMETER })
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidRole {
    String message() default "The role you have entered is not permitted";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    class ValidRoleValidator implements ConstraintValidator<ValidRole, Long> {

        @Override
        public boolean isValid(Long value, ConstraintValidatorContext context) {
            if(value == null) return true;
            return value.equals(ERole.ASSISTANT.getId()) || value.equals(ERole.DOCTOR.getId());
        }
    }
}
