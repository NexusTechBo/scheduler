package com.example.scheduler.validator.specialtyRole;

import com.example.scheduler.enums.ERole;
import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = SpecialtyRoleMatch.SpecialtyRoleMatchValidator.class)
@Target({ ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
public @interface SpecialtyRoleMatch {
    String message() default "Invalid specialty for the given role";

    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};

    // Implemented by the requests annotated with @SpecialtyRoleMatch; record accessors satisfy it
    interface Request {
        ERole role();
        Long specialtyId();
    }

    class SpecialtyRoleMatchValidator implements ConstraintValidator<SpecialtyRoleMatch, Request> {

        @Override
        public boolean isValid(Request value, ConstraintValidatorContext context) {
            if (value == null || value.role() == null) return true;
            var existsSpecialty = value.specialtyId() != null;
            if (value.role() == ERole.DOCTOR && !existsSpecialty) {
                buildViolation(context, "The doctor role should have a specialty");
                return false;
            }
            if (value.role() == ERole.ASSISTANT && existsSpecialty) {
                buildViolation(context, "Only doctors can have a specialty");
                return false;
            }
            return true;
        }

        private void buildViolation(ConstraintValidatorContext context, String errorMessage) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(errorMessage)
                    .addPropertyNode("specialtyId")
                    .addConstraintViolation();
        }
    }
}
