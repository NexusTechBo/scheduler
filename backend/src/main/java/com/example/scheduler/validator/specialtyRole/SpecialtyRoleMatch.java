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
    String message() default "{validation.specialtyRole.mismatch}";

    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};

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
                buildViolation(context, "{validation.specialtyRole.doctorRequired}");
                return false;
            }
            if (value.role() == ERole.ASSISTANT && existsSpecialty) {
                buildViolation(context, "{validation.specialtyRole.mismatch}");
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
