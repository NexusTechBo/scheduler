package com.example.scheduler.dto.clinic;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ClinicRequestValidationTest {
    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void acceptsRegularClinicName() {
        assertThat(validator.validate(request("Clínica Sur & Asociados"))).isEmpty();
    }

    @Test
    void rejectsControlCharactersInName() {
        assertThat(validator.validate(request("Evil\r\nBcc: x@y.com")))
                .extracting(v -> v.getPropertyPath().toString(), ConstraintViolation::getMessageTemplate)
                .containsExactly(org.assertj.core.groups.Tuple.tuple("name", "{validation.noControlChars}"));
        assertThat(validator.validate(request("Tab\there")))
                .extracting(v -> v.getPropertyPath().toString())
                .containsExactly("name");
    }

    @Test
    void rejectsNameLongerThan100() {
        assertThat(validator.validate(request("a".repeat(101))))
                .extracting(v -> v.getPropertyPath().toString())
                .containsExactly("name");
    }

    private static ClinicRequest request(String name) {
        return new ClinicRequest(
                name,
                "clinicasur",
                "70000000",
                "Ana",
                "admin",
                "ana@mail.com",
                "password1",
                "1234567"
        );
    }
}
