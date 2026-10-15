package com.example.scheduler.validator.specialtyRole;

import com.example.scheduler.enums.ERole;
import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;

import java.lang.annotation.*;
import java.lang.reflect.Method;

@Documented
@Constraint(validatedBy = SpecialtyRoleMatch.SpecialtyRoleMatchValidator.class)
@Target({ ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
public @interface SpecialtyRoleMatch {
    String message() default "";

    String first();
    String second();

    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};

    @Target({ ElementType.TYPE })
    @Retention(RetentionPolicy.RUNTIME)
    @Documented
    @interface List {
        SpecialtyRoleMatch[] value();
    }

    class SpecialtyRoleMatchValidator implements ConstraintValidator<SpecialtyRoleMatch, Object> {
        private String firstField;
        private String secondField;

        @Override
        public void initialize(SpecialtyRoleMatch constraintAnnotation) {
            this.firstField = constraintAnnotation.first();
            this.secondField = constraintAnnotation.second();
        }

        @Override
        public boolean isValid(Object value, ConstraintValidatorContext context) {
            if (value == null) return true;
            try {
                var roleId = (Long) getFieldValue(value, firstField);
                var specialtyId = (Long) getFieldValue(value, secondField);
                if (roleId == null) return true;
                var isDoctor = roleId.equals(ERole.DOCTOR.getId());
                var isAssistant = roleId.equals(ERole.ASSISTANT.getId());
                var existsSpecialty = specialtyId != null;
                if (isDoctor && !existsSpecialty) {
                    var errorMessage = "The doctor role should have a specialty";
                    buildViolation(context, errorMessage, secondField);
                    return false;
                }
                if (isAssistant && existsSpecialty) {
                    var errorMessage = "The assistant role shouldn't have a specialty";
                    buildViolation(context, errorMessage, secondField);
                    return false;
                }

                return true;
            } catch (Exception e) {
                return false;
            }
        }

        private void buildViolation(ConstraintValidatorContext context, String errorMessage, String node) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(errorMessage)
                    .addPropertyNode(node)
                    .addConstraintViolation();
        }

        private Object getFieldValue(Object object, String fieldName) throws Exception {
            var clazz = object.getClass();
            try {
                var recordMethod = clazz.getMethod(fieldName);
                return recordMethod.invoke(object);
            } catch (NoSuchMethodException ignored) {}
            var getterName = "get" + fieldName.substring(0, 1).toUpperCase() + fieldName.substring(1);
            var getterMethod = clazz.getMethod(getterName);
            return getterMethod.invoke(object);
        }
    }
}
