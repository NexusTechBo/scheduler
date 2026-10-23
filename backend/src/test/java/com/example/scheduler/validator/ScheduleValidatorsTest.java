package com.example.scheduler.validator;

import com.example.scheduler.dto.DoctorAvailability.DoctorAvailabilityRequest;
import com.example.scheduler.dto.ScheduleException.ScheduleExceptionRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;

class ScheduleValidatorsTest {
    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
    private static final LocalTime NINE = LocalTime.of(9, 0);
    private static final LocalTime TEN = LocalTime.of(10, 0);

    @Test
    void timeRangeRejectsEndNotAfterStartOnEndField() {
        assertThat(validator.validate(new DoctorAvailabilityRequest(DayOfWeek.MONDAY, TEN, NINE, 30)))
                .extracting(v -> v.getPropertyPath().toString(), ConstraintViolation::getMessage)
                .containsExactly(org.assertj.core.groups.Tuple.tuple("endTime", "End time must be after start time"));
        assertThat(validator.validate(new DoctorAvailabilityRequest(DayOfWeek.MONDAY, NINE, NINE, 30)))
                .extracting(v -> v.getPropertyPath().toString())
                .containsExactly("endTime");
        assertThat(validator.validate(new DoctorAvailabilityRequest(DayOfWeek.MONDAY, NINE, TEN, 30))).isEmpty();
    }

    @Test
    void partialBlockRequiresBothTimes() {
        var date = LocalDate.of(2030, 1, 1);
        assertThat(validator.validate(new ScheduleExceptionRequest(date, null, null, false, null, null)))
                .extracting(v -> v.getPropertyPath().toString())
                .containsExactlyInAnyOrder("startTime", "endTime");
        assertThat(validator.validate(new ScheduleExceptionRequest(date, NINE, null, false, null, null)))
                .extracting(v -> v.getPropertyPath().toString())
                .containsExactly("endTime");
        assertThat(validator.validate(new ScheduleExceptionRequest(date, null, null, true, null, null))).isEmpty();
        assertThat(validator.validate(new ScheduleExceptionRequest(date, NINE, TEN, false, null, null))).isEmpty();
        assertThat(validator.validate(new ScheduleExceptionRequest(date, TEN, NINE, false, null, null)))
                .extracting(v -> v.getPropertyPath().toString())
                .containsExactly("endTime");
    }
}
