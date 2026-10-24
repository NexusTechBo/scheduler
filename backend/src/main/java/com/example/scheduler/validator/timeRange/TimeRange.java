package com.example.scheduler.validator.timeRange;

import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;

import java.lang.annotation.*;
import java.time.LocalTime;

@Documented
@Constraint(validatedBy = TimeRange.TimeRangeValidator.class)
@Target({ ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
public @interface TimeRange {
    String message() default "{validation.timeRange.endAfterStart}";

    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};

    // Implemented by the requests annotated with @TimeRange; record accessors satisfy it
    interface Request {
        LocalTime startTime();
        LocalTime endTime();
    }

    class TimeRangeValidator implements ConstraintValidator<TimeRange, Request> {

        @Override
        public boolean isValid(Request value, ConstraintValidatorContext context) {
            if (value == null) return true;
            var start = value.startTime();
            var end = value.endTime();
            if (start == null || end == null || end.isAfter(start)) return true;
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(context.getDefaultConstraintMessageTemplate())
                    .addPropertyNode("endTime")
                    .addConstraintViolation();
            return false;
        }
    }
}
