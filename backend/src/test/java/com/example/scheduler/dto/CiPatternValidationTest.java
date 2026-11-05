package com.example.scheduler.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.scheduler.config.LocaleConfig;
import com.example.scheduler.dto.clinic.ClinicRequest;
import com.example.scheduler.dto.guest.GuestNewPatientRequest;
import com.example.scheduler.dto.guest.GuestPatientRequest;
import com.example.scheduler.dto.patient.PatientRequest;
import com.example.scheduler.dto.personal.PersonalRequest;
import com.example.scheduler.enums.ERole;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

class CiPatternValidationTest {
    private static final String CI_PATTERN_MESSAGE = "debe contener solo números, entre 5 y 10 dígitos";

    private static final List<Target> TARGETS = List.of(
            new Target("PatientRequest", "ci", ci -> new PatientRequest(ci, "Ana Perez", "ana@mail.test", "70000000")),
            new Target(
                    "PersonalRequest",
                    "ci",
                    ci -> new PersonalRequest(
                            "Dr House", "house@mail.test", "70000000", ci, "house", "secret123", ERole.DOCTOR, 1L)),
            new Target("GuestPatientRequest", "ci", ci -> new GuestPatientRequest(ci, "70000000")),
            new Target(
                    "GuestNewPatientRequest",
                    "ci",
                    ci -> new GuestNewPatientRequest(ci, "Ana Perez", "70000000", "ana@mail.test")),
            new Target(
                    "ClinicRequest",
                    "adminCi",
                    ci -> new ClinicRequest(
                            "Clinica Demo",
                            "demo",
                            "70000000",
                            "Admin Demo",
                            "admin",
                            "admin@mail.test",
                            "secret123",
                            ci)));

    private static LocalValidatorFactoryBean factory;
    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        factory = new LocaleConfig().getValidator();
        factory.afterPropertiesSet();
        validator = factory.getValidator();
    }

    @AfterAll
    static void closeValidator() {
        factory.close();
    }

    @BeforeEach
    void useSpanishLocale() {
        LocaleContextHolder.setLocale(LocaleConfig.SPANISH);
    }

    @AfterEach
    void resetLocale() {
        LocaleContextHolder.resetLocaleContext();
    }

    @ParameterizedTest(name = "{0} rejects \"{1}\"")
    @MethodSource("malformedCis")
    void shouldRejectWithCiPatternMessageWhenCiIsMalformed(Target target, String ci) {
        var violations = violationsOn(target, ci);

        assertThat(violations).hasSize(1);
        var violation = violations.iterator().next();
        assertThat(annotationOf(violation)).isEqualTo(Pattern.class);
        assertThat(violation.getMessage()).isEqualTo(CI_PATTERN_MESSAGE);
        assertThat(violation.getPropertyPath()).hasToString(target.property());
    }

    @ParameterizedTest(name = "{0} rejects blank \"{1}\"")
    @MethodSource("blankCis")
    void shouldRejectAsRequiredWhenCiIsBlank(Target target, String ci) {
        var violations = violationsOn(target, ci);

        assertThat(violations).extracting(CiPatternValidationTest::annotationOf).contains(NotBlank.class);
        assertThat(violations).extracting(v -> v.getPropertyPath().toString()).containsOnly(target.property());
    }

    @ParameterizedTest(name = "{0} rejects null")
    @MethodSource("targets")
    void shouldRejectOnlyAsRequiredWhenCiIsNull(Target target) {
        var violations = violationsOn(target, null);

        assertThat(violations).extracting(CiPatternValidationTest::annotationOf).containsExactly(NotBlank.class);
    }

    @ParameterizedTest(name = "{0} accepts \"{1}\"")
    @MethodSource("acceptedCis")
    void shouldAcceptWhenCiHasBetweenFiveAndTenDigits(Target target, String ci) {
        assertThat(validator.validate(target.build().apply(ci))).isEmpty();
    }

    static Stream<Arguments> malformedCis() {
        return combine("1234", "12345678901", "12a456", "12 345", "-12345");
    }

    static Stream<Arguments> blankCis() {
        return combine("", "   ");
    }

    static Stream<Arguments> acceptedCis() {
        return combine("12345", "1234567890");
    }

    static Stream<Arguments> targets() {
        return TARGETS.stream().map(Arguments::of);
    }

    private static Stream<Arguments> combine(String... values) {
        return TARGETS.stream().flatMap(t -> Stream.of(values).map(v -> Arguments.of(t, v)));
    }

    private static Set<ConstraintViolation<Object>> violationsOn(Target target, String ci) {
        Set<ConstraintViolation<Object>> all = validator.validate(target.build().apply(ci));
        assertThat(all)
                .as("the rest of the %s fixture must be valid", target.name())
                .allSatisfy(v -> assertThat(v.getPropertyPath()).hasToString(target.property()));
        return all;
    }

    private static Object annotationOf(ConstraintViolation<?> violation) {
        return violation.getConstraintDescriptor().getAnnotation().annotationType();
    }

    private record Target(String name, String property, Function<String, Object> build) {
        @Override
        public String toString() {
            return name + "." + property;
        }
    }
}
