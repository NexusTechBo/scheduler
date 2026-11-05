package com.example.scheduler.exception;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

import com.example.scheduler.config.LocaleConfig;
import com.example.scheduler.enums.AppointmentStatus;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.access.AccessDeniedException;

class GlobalExceptionHandlerTest {
    private GlobalExceptionHandler handler;
    private MockHttpServletRequest request;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler(new LocaleConfig().messageSource());
        request = new MockHttpServletRequest("POST", "/api/appointments");
        LocaleContextHolder.setLocale(LocaleConfig.SPANISH);
    }

    @AfterEach
    void tearDown() {
        LocaleContextHolder.resetLocaleContext();
    }

    @Test
    void shouldReturnCodeMessageAndDetailsWhenApiExceptionCarriesDetails() {
        var start = LocalDateTime.of(2026, 11, 15, 14, 30);
        var details = new LinkedHashMap<String, Object>();
        details.put("startTime", start);
        details.put("appointmentId", 555L);
        details.put("status", AppointmentStatus.CONFIRMED);
        var ex = BusinessException.withDetails(
                details, MessageKeys.APPOINTMENT_PATIENT_HAS_ACTIVE, "15/11/2026", "14:30", 1);

        var response = handler.handleApi(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        var body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.status()).isEqualTo(409);
        assertThat(body.error()).isEqualTo("Conflict");
        assertThat(body.code()).isEqualTo(MessageKeys.APPOINTMENT_PATIENT_HAS_ACTIVE);
        assertThat(body.message())
                .isEqualTo("El paciente ya tiene una cita confirmada con este doctor el 15/11/2026 a las 14:30.");
        assertThat(body.details())
                .containsExactly(
                        entry("startTime", start),
                        entry("appointmentId", 555L),
                        entry("status", AppointmentStatus.CONFIRMED));
        assertThat(body.path()).isEqualTo("/api/appointments");
        assertThat(body.errors()).isNull();
    }

    @Test
    void shouldRenderPendingLabelWhenStatusChoiceIsZero() {
        var ex = BusinessException.withDetails(
                Map.of("startTime", LocalDateTime.of(2026, 11, 15, 14, 30)),
                MessageKeys.APPOINTMENT_PATIENT_HAS_ACTIVE,
                "15/11/2026",
                "14:30",
                0);

        var body = handler.handleApi(ex, request).getBody();

        assertThat(body).isNotNull();
        assertThat(body.message())
                .isEqualTo("El paciente ya tiene una cita pendiente con este doctor el 15/11/2026 a las 14:30.");
    }

    @Test
    void shouldResolveGuestConflictMessage() {
        var ex = BusinessException.withDetails(
                Map.of("startTime", LocalDateTime.of(2026, 11, 15, 14, 30)),
                MessageKeys.APPOINTMENT_PATIENT_HAS_ACTIVE_GUEST,
                "15/11/2026",
                "14:30");

        var body = handler.handleApi(ex, request).getBody();

        assertThat(body).isNotNull();
        assertThat(body.message())
                .isEqualTo("Ya tienes una cita con este doctor el 15/11/2026 a las 14:30."
                        + " Comunícate con la clínica para reprogramarla.");
        assertThat(body.details()).containsOnlyKeys("startTime");
    }

    @Test
    void shouldResolveSingularAndPluralScheduleConflictMessages() {
        var single = handler.handleApi(
                        BusinessException.withDetails(
                                Map.of("count", 1L), MessageKeys.SCHEDULE_EXCEPTION_ACTIVE_APPOINTMENTS_CONFLICT, 1L),
                        request)
                .getBody();
        var many = handler.handleApi(
                        BusinessException.withDetails(
                                Map.of("count", 3L), MessageKeys.SCHEDULE_EXCEPTION_ACTIVE_APPOINTMENTS_CONFLICT, 3L),
                        request)
                .getBody();

        assertThat(single).isNotNull();
        assertThat(many).isNotNull();
        assertThat(single.message()).startsWith("Hay 1 cita activa");
        assertThat(many.message()).startsWith("Hay 3 citas activas");
        assertThat(many.details()).containsExactly(entry("count", 3L));
    }

    @Test
    void shouldResolveRescheduleMessages() {
        var onlyActive = handler.handleApi(
                        new BusinessException(MessageKeys.APPOINTMENT_RESCHEDULE_ONLY_ACTIVE), request)
                .getBody();
        var sameTime = handler.handleApi(new BusinessException(MessageKeys.APPOINTMENT_RESCHEDULE_SAME_TIME), request)
                .getBody();
        var past = handler.handleApi(new BusinessException(MessageKeys.APPOINTMENT_PAST_NOT_MODIFIABLE), request)
                .getBody();

        assertThat(onlyActive).isNotNull();
        assertThat(sameTime).isNotNull();
        assertThat(past).isNotNull();
        assertThat(onlyActive.message()).isEqualTo("Solo puedes reprogramar citas pendientes o confirmadas.");
        assertThat(sameTime.message()).isEqualTo("La cita ya está agendada en ese horario.");
        assertThat(past.message()).isEqualTo("No puedes confirmar, cancelar ni reprogramar una cita que ya comenzó.");
    }

    @Test
    void shouldSetCodeButOmitDetailsWhenApiExceptionHasNoDetails() {
        var ex = new ResourceNotFoundException(MessageKeys.APPOINTMENT_NOT_FOUND, 30L);

        var response = handler.handleApi(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        var body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.code()).isEqualTo(MessageKeys.APPOINTMENT_NOT_FOUND);
        assertThat(body.message()).isEqualTo("No se encontró la cita con el identificador: 30.");
        assertThat(body.details()).isNull();
    }

    @Test
    void shouldFallBackToKeyWhenMessageIsMissing() {
        var ex = new BusinessException("does.not.exist");

        var body = handler.handleApi(ex, request).getBody();

        assertThat(body).isNotNull();
        assertThat(body.message()).isEqualTo("does.not.exist");
        assertThat(body.code()).isEqualTo("does.not.exist");
    }

    @Test
    void shouldReturnForbiddenWithoutCodeWhenAccessDenied() {
        var response = handler.handleAccessDenied(new AccessDeniedException("denied"), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        var body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.code()).isNull();
        assertThat(body.details()).isNull();
        assertThat(body.message()).isEqualTo("No tienes permiso para acceder a este recurso.");
    }

    @Test
    void shouldReturnGenericInternalErrorWithoutCodeWhenUnexpected() {
        var response = handler.handleUnexpected(new IllegalStateException("boom"), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        var body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.code()).isNull();
        assertThat(body.message()).doesNotContain("boom");
    }
}
