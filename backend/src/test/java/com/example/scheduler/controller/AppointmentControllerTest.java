package com.example.scheduler.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.scheduler.config.JacksonConfig;
import com.example.scheduler.config.LocaleConfig;
import com.example.scheduler.config.SecurityConfig;
import com.example.scheduler.dto.appointment.AppointmentRequest;
import com.example.scheduler.dto.appointment.AppointmentResponse;
import com.example.scheduler.dto.appointment.CancelAppointmentRequest;
import com.example.scheduler.dto.appointment.RescheduleAppointmentRequest;
import com.example.scheduler.enums.AppointmentStatus;
import com.example.scheduler.exception.BusinessException;
import com.example.scheduler.exception.ErrorResponseWriter;
import com.example.scheduler.exception.ForbiddenException;
import com.example.scheduler.exception.MessageKeys;
import com.example.scheduler.exception.ResourceNotFoundException;
import com.example.scheduler.ratelimiting.RateLimitProperties;
import com.example.scheduler.security.ActiveUserChecker;
import com.example.scheduler.security.jwt.JwtClaims;
import com.example.scheduler.security.jwt.JwtUtil;
import com.example.scheduler.service.AppointmentService;
import io.jsonwebtoken.Jwts;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

@WebMvcTest(controllers = AppointmentController.class, properties = "rate-limit.enabled=false")
@Import({
    SecurityConfig.class,
    LocaleConfig.class,
    JacksonConfig.class,
    ErrorResponseWriter.class,
    AppointmentControllerTest.SliceSecurityConfig.class
})
class AppointmentControllerTest {
    private static final String ADMIN_TOKEN = "admin-token";
    private static final String DOCTOR_TOKEN = "doctor-token";
    private static final String ADMIN_DOCTOR_TOKEN = "admin-doctor-token";
    private static final String ASSISTANT_TOKEN = "assistant-token";
    private static final String GUEST_TOKEN = "guest-token";
    private static final long ADMIN_ID = 1L;
    private static final long DOCTOR_ID = 10L;
    private static final long ADMIN_DOCTOR_ID = 2L;
    private static final long ASSISTANT_ID = 3L;
    private static final LocalDateTime START = LocalDateTime.of(2026, 11, 10, 9, 0);

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private AppointmentService appointmentService;

    @MockitoBean
    private JwtUtil jwtUtil;

    @MockitoBean
    private ActiveUserChecker activeUserChecker;

    @BeforeEach
    void setUpTokens() {
        givenToken(ADMIN_TOKEN, ADMIN_ID, "ADMINISTRATOR", List.of("ADMINISTRATOR"));
        givenToken(DOCTOR_TOKEN, DOCTOR_ID, "DOCTOR", List.of("DOCTOR"));
        givenToken(ADMIN_DOCTOR_TOKEN, ADMIN_DOCTOR_ID, "ADMINISTRATOR", List.of("ADMINISTRATOR", "DOCTOR"));
        givenToken(ASSISTANT_TOKEN, ASSISTANT_ID, "ASSISTANT", List.of("ASSISTANT"));
        givenToken(GUEST_TOKEN, 0L, "GUEST", List.of());
        when(activeUserChecker.isActive(anyLong(), anyString(), anyCollection()))
                .thenReturn(true);
    }

    private void givenToken(String token, long userId, String role, List<String> roles) {
        var builder = Jwts.claims()
                .subject(Long.toString(userId))
                .add(JwtClaims.ROLE, role)
                .add(JwtClaims.CLINIC_ID, 7L)
                .add(JwtClaims.CLINIC_SLUG, "demo");
        if (!roles.isEmpty()) builder.add(JwtClaims.ROLES, roles);
        when(jwtUtil.parse(token)).thenReturn(Optional.of(builder.build()));
    }

    private static MockHttpServletRequestBuilder as(String token, MockHttpServletRequestBuilder builder) {
        return builder.header(HttpHeaders.AUTHORIZATION, "Bearer " + token);
    }

    private static AppointmentResponse response(Long id, LocalDateTime start, AppointmentStatus status) {
        return new AppointmentResponse(
                id,
                start,
                start.plusMinutes(30),
                DOCTOR_ID,
                "Dr House",
                "Cardiologia",
                "house@mail.test",
                20L,
                "Ana",
                "ana@mail.test",
                status,
                null,
                LocalDateTime.of(2026, 11, 1, 8, 0));
    }

    @TestConfiguration
    @EnableConfigurationProperties(RateLimitProperties.class)
    static class SliceSecurityConfig {}

    @Nested
    class Reschedule {
        @Test
        void shouldReturnOkWithBodyWhenAdministratorReschedules() throws Exception {
            when(appointmentService.rescheduleAppointmentById(
                            eq(30L), any(RescheduleAppointmentRequest.class), anyLong(), anyBoolean()))
                    .thenReturn(response(30L, START, AppointmentStatus.PENDING));

            mvc.perform(as(ADMIN_TOKEN, patch("/api/appointments/30/reschedule"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"startTime\":\"2026-11-10T09:00:00\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(30))
                    .andExpect(jsonPath("$.startTime").value("2026-11-10T09:00:00"))
                    .andExpect(jsonPath("$.status").value("PENDING"))
                    .andExpect(jsonPath("$.doctorId").value(DOCTOR_ID));

            verify(appointmentService)
                    .rescheduleAppointmentById(30L, new RescheduleAppointmentRequest(START), ADMIN_ID, false);
        }

        @Test
        void shouldRestrictToOwnDataWhenDoctorReschedules() throws Exception {
            mvc.perform(as(DOCTOR_TOKEN, patch("/api/appointments/30/reschedule"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"startTime\":\"2026-11-10T09:00:00\"}"))
                    .andExpect(status().isOk());

            verify(appointmentService)
                    .rescheduleAppointmentById(30L, new RescheduleAppointmentRequest(START), DOCTOR_ID, true);
        }

        @Test
        void shouldNotRestrictWhenAdministratorIsAlsoDoctor() throws Exception {
            mvc.perform(as(ADMIN_DOCTOR_TOKEN, patch("/api/appointments/30/reschedule"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"startTime\":\"2026-11-10T09:00:00\"}"))
                    .andExpect(status().isOk());

            verify(appointmentService)
                    .rescheduleAppointmentById(30L, new RescheduleAppointmentRequest(START), ADMIN_DOCTOR_ID, false);
        }

        @Test
        void shouldAllowAssistantToReschedule() throws Exception {
            mvc.perform(as(ASSISTANT_TOKEN, patch("/api/appointments/30/reschedule"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"startTime\":\"2026-11-10T09:00:00\"}"))
                    .andExpect(status().isOk());

            verify(appointmentService)
                    .rescheduleAppointmentById(30L, new RescheduleAppointmentRequest(START), ASSISTANT_ID, false);
        }

        @Test
        void shouldReturnBadRequestWithFieldErrorWhenStartTimeIsMissing() throws Exception {
            mvc.perform(as(ADMIN_TOKEN, patch("/api/appointments/30/reschedule"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400))
                    .andExpect(jsonPath("$.message").value("Error de validación en los datos enviados."))
                    .andExpect(jsonPath("$.errors", hasSize(1)))
                    .andExpect(jsonPath("$.errors[0].field").value("startTime"))
                    .andExpect(jsonPath("$.errors[0].message").isNotEmpty())
                    .andExpect(jsonPath("$.code").doesNotExist())
                    .andExpect(jsonPath("$.details").doesNotExist())
                    .andExpect(jsonPath("$.path").value("/api/appointments/30/reschedule"));

            verifyNoInteractions(appointmentService);
        }

        @Test
        void shouldReturnBadRequestWhenStartTimeIsMalformed() throws Exception {
            mvc.perform(as(ADMIN_TOKEN, patch("/api/appointments/30/reschedule"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"startTime\":\"mañana\"}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("El cuerpo de la solicitud no tiene un formato válido."));

            verifyNoInteractions(appointmentService);
        }

        @Test
        void shouldReturnBadRequestWhenPathVariableIsNotNumeric() throws Exception {
            mvc.perform(as(ADMIN_TOKEN, patch("/api/appointments/abc/reschedule"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"startTime\":\"2026-11-10T09:00:00\"}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message")
                            .value("El valor ingresado para el parámetro 'appointmentId' no es válido."));
        }

        @Test
        void shouldReturnConflictWithCodeWhenRescheduleIsRejected() throws Exception {
            when(appointmentService.rescheduleAppointmentById(anyLong(), any(), anyLong(), anyBoolean()))
                    .thenThrow(new BusinessException(MessageKeys.APPOINTMENT_RESCHEDULE_SAME_TIME));

            mvc.perform(as(ADMIN_TOKEN, patch("/api/appointments/30/reschedule"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"startTime\":\"2026-11-10T09:00:00\"}"))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.status").value(409))
                    .andExpect(jsonPath("$.error").value("Conflict"))
                    .andExpect(jsonPath("$.code").value("appointment.rescheduleSameTime"))
                    .andExpect(jsonPath("$.message").value("La cita ya está agendada en ese horario."))
                    .andExpect(jsonPath("$.details").doesNotExist())
                    .andExpect(jsonPath("$.errors").doesNotExist())
                    .andExpect(jsonPath("$.path").value("/api/appointments/30/reschedule"));
        }

        @Test
        void shouldReturnNotFoundWithCodeWhenAppointmentDoesNotExist() throws Exception {
            when(appointmentService.rescheduleAppointmentById(anyLong(), any(), anyLong(), anyBoolean()))
                    .thenThrow(new ResourceNotFoundException(MessageKeys.APPOINTMENT_NOT_FOUND, 30L));

            mvc.perform(as(ADMIN_TOKEN, patch("/api/appointments/30/reschedule"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"startTime\":\"2026-11-10T09:00:00\"}"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value("appointment.notFound"))
                    .andExpect(jsonPath("$.message").value("No se encontró la cita con el identificador: 30."));
        }

        @Test
        void shouldReturnForbiddenWithCodeWhenDoctorReschedulesAnotherDoctorsAppointment() throws Exception {
            when(appointmentService.rescheduleAppointmentById(anyLong(), any(), anyLong(), anyBoolean()))
                    .thenThrow(new ForbiddenException(MessageKeys.APPOINTMENT_UNAUTHORIZED));

            mvc.perform(as(DOCTOR_TOKEN, patch("/api/appointments/30/reschedule"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"startTime\":\"2026-11-10T09:00:00\"}"))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.code").value("appointment.unauthorized"))
                    .andExpect(jsonPath("$.message").value("No tienes autorización para realizar esta acción."));
        }

        @Test
        void shouldReturnUnauthorizedWithoutToken() throws Exception {
            mvc.perform(patch("/api/appointments/30/reschedule")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"startTime\":\"2026-11-10T09:00:00\"}"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.status").value(401))
                    .andExpect(jsonPath("$.message").value("Debes iniciar sesión para acceder a este recurso."))
                    .andExpect(jsonPath("$.code").doesNotExist());

            verifyNoInteractions(appointmentService);
        }

        @Test
        void shouldReturnUnauthorizedWhenUserIsNoLongerActive() throws Exception {
            when(activeUserChecker.isActive(eq(DOCTOR_ID), anyString(), anyCollection()))
                    .thenReturn(false);

            mvc.perform(as(DOCTOR_TOKEN, patch("/api/appointments/30/reschedule"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"startTime\":\"2026-11-10T09:00:00\"}"))
                    .andExpect(status().isUnauthorized());

            verifyNoInteractions(appointmentService);
        }

        @Test
        void shouldReturnForbiddenForGuestToken() throws Exception {
            mvc.perform(as(GUEST_TOKEN, patch("/api/appointments/30/reschedule"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"startTime\":\"2026-11-10T09:00:00\"}"))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.status").value(403))
                    .andExpect(jsonPath("$.message").value("No tienes permiso para acceder a este recurso."));

            verifyNoInteractions(appointmentService);
        }
    }

    @Nested
    class Book {
        @Test
        void shouldReturnCreatedWithBodyWhenBooking() throws Exception {
            when(appointmentService.bookAppointment(any(), anyLong(), anyBoolean()))
                    .thenReturn(response(99L, START, AppointmentStatus.PENDING));

            mvc.perform(as(ASSISTANT_TOKEN, post("/api/appointments"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"doctorId\":10,\"patientId\":20,\"startTime\":\"2026-11-10T09:00:00\"}"))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").value(99))
                    .andExpect(jsonPath("$.status").value("PENDING"));

            verify(appointmentService).bookAppointment(new AppointmentRequest(10L, 20L, START), ASSISTANT_ID, false);
        }

        @Test
        void shouldReturnBadRequestWithAllFieldErrorsWhenBodyIsEmpty() throws Exception {
            mvc.perform(as(ADMIN_TOKEN, post("/api/appointments"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors", hasSize(3)))
                    .andExpect(jsonPath("$.errors[*].field")
                            .value(containsInAnyOrder("doctorId", "patientId", "startTime")));

            verifyNoInteractions(appointmentService);
        }

        @Test
        @DisplayName(
                "should return 409 with code, Spanish message and details when patient already has an active appointment")
        void shouldReturnConflictWithDetailsWhenPatientHasActiveAppointment() throws Exception {
            var details = new LinkedHashMap<String, Object>();
            details.put("startTime", LocalDateTime.of(2026, 11, 15, 14, 30));
            details.put("appointmentId", 555L);
            details.put("status", AppointmentStatus.CONFIRMED);
            when(appointmentService.bookAppointment(any(), anyLong(), anyBoolean()))
                    .thenThrow(BusinessException.withDetails(
                            details, MessageKeys.APPOINTMENT_PATIENT_HAS_ACTIVE, "15/11/2026", "14:30", 1));

            mvc.perform(as(ADMIN_TOKEN, post("/api/appointments"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"doctorId\":10,\"patientId\":20,\"startTime\":\"2026-11-10T09:00:00\"}"))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.code").value("appointment.patientHasActive"))
                    .andExpect(
                            jsonPath("$.message")
                                    .value(
                                            "El paciente ya tiene una cita confirmada con este doctor el 15/11/2026 a las 14:30."))
                    .andExpect(jsonPath("$.details.startTime").value("2026-11-15T14:30:00"))
                    .andExpect(jsonPath("$.details.appointmentId").value(555))
                    .andExpect(jsonPath("$.details.status").value("CONFIRMED"));
        }

        @Test
        void shouldPassRestrictionFlagWhenDoctorBooks() throws Exception {
            mvc.perform(as(DOCTOR_TOKEN, post("/api/appointments"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"doctorId\":10,\"patientId\":20,\"startTime\":\"2026-11-10T09:00:00\"}"))
                    .andExpect(status().isCreated());

            verify(appointmentService).bookAppointment(new AppointmentRequest(10L, 20L, START), DOCTOR_ID, true);
        }
    }

    @Nested
    class OtherEndpoints {
        @Test
        void shouldReturnAppointmentWhenFindingById() throws Exception {
            when(appointmentService.findAppointmentById(30L, ADMIN_ID, false))
                    .thenReturn(response(30L, START, AppointmentStatus.CONFIRMED));

            mvc.perform(as(ADMIN_TOKEN, get("/api/appointments/30")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("CONFIRMED"));
        }

        @Test
        void shouldConfirmWithRestrictionForDoctor() throws Exception {
            mvc.perform(as(DOCTOR_TOKEN, patch("/api/appointments/30/confirm"))).andExpect(status().isOk());

            verify(appointmentService).confirmAppointmentById(30L, DOCTOR_ID, true);
        }

        @Test
        void shouldCancelWithReason() throws Exception {
            mvc.perform(as(ADMIN_TOKEN, patch("/api/appointments/30/cancel"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"reason\":\"  Viaje  \"}"))
                    .andExpect(status().isOk());

            verify(appointmentService)
                    .cancelAppointmentById(30L, new CancelAppointmentRequest("Viaje"), ADMIN_ID, false);
        }

        @Test
        void shouldReturnBadRequestWhenCancelReasonIsBlank() throws Exception {
            mvc.perform(as(ADMIN_TOKEN, patch("/api/appointments/30/cancel"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"reason\":\"   \"}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors[0].field").value("reason"));

            verifyNoInteractions(appointmentService);
        }

        @Test
        void shouldPassFiltersPaginationAndSortingWhenListing() throws Exception {
            when(appointmentService.findAllAppointments(any(), any(), any(), any()))
                    .thenReturn(new PageImpl<>(
                            List.of(response(1L, START, AppointmentStatus.PENDING)), PageRequest.of(1, 5), 6));

            mvc.perform(as(ADMIN_TOKEN, get("/api/appointments"))
                            .param("doctorId", "10")
                            .param("patientId", "20")
                            .param("status", "CONFIRMED")
                            .param("page", "1")
                            .param("size", "5")
                            .param("sort", "endTime,desc"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].id").value(1))
                    .andExpect(jsonPath("$.page.totalElements").value(6));

            var pageable = ArgumentCaptor.forClass(Pageable.class);
            verify(appointmentService)
                    .findAllAppointments(eq(10L), eq(20L), eq(AppointmentStatus.CONFIRMED), pageable.capture());
            assertThat(pageable.getValue().getPageNumber()).isEqualTo(1);
            assertThat(pageable.getValue().getPageSize()).isEqualTo(5);
            assertThat(pageable.getValue().getSort()).isEqualTo(Sort.by(Sort.Direction.DESC, "endTime"));
        }

        @Test
        void shouldSortByStartTimeAscendingByDefaultWhenListing() throws Exception {
            when(appointmentService.findAllAppointments(any(), any(), any(), any()))
                    .thenReturn(new PageImpl<>(List.of()));

            mvc.perform(as(ADMIN_TOKEN, get("/api/appointments"))).andExpect(status().isOk());

            var pageable = ArgumentCaptor.forClass(Pageable.class);
            verify(appointmentService).findAllAppointments(isNull(), isNull(), isNull(), pageable.capture());
            assertThat(pageable.getValue().getSort()).isEqualTo(Sort.by(Sort.Direction.ASC, "startTime"));
        }

        @Test
        void shouldForceOwnDoctorIdWhenDoctorListsAppointments() throws Exception {
            when(appointmentService.findAllAppointments(any(), any(), any(), any()))
                    .thenReturn(new PageImpl<>(List.of()));

            mvc.perform(as(DOCTOR_TOKEN, get("/api/appointments")).param("doctorId", "999"))
                    .andExpect(status().isOk());

            verify(appointmentService).findAllAppointments(eq(DOCTOR_ID), isNull(), isNull(), any());
        }

        @Test
        void shouldReturnBadRequestWhenStatusFilterIsUnknown() throws Exception {
            mvc.perform(as(ADMIN_TOKEN, get("/api/appointments")).param("status", "DONE"))
                    .andExpect(status().isBadRequest())
                    .andExpect(
                            jsonPath("$.message").value("El valor ingresado para el parámetro 'status' no es válido."));
        }

        @Test
        void shouldPassDateRangeAndScopedDoctorWhenGettingBoard() throws Exception {
            when(appointmentService.getBoardByRange(any(), any(), any(), any())).thenReturn(Map.of());

            mvc.perform(as(DOCTOR_TOKEN, get("/api/appointments/board"))
                            .param("from", "2026-11-01")
                            .param("to", "2026-11-30")
                            .param("patientId", "20"))
                    .andExpect(status().isOk());

            verify(appointmentService)
                    .getBoardByRange(LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 30), DOCTOR_ID, 20L);
        }

        @Test
        void shouldReturnBadRequestWhenBoardDateIsMissing() throws Exception {
            mvc.perform(as(ADMIN_TOKEN, get("/api/appointments/board")).param("from", "2026-11-01"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("Falta el parámetro requerido 'to'."));
        }

        @Test
        void shouldReturnBadRequestWithFieldErrorWhenCalendarMonthIsOutOfRange() throws Exception {
            mvc.perform(as(ADMIN_TOKEN, get("/api/appointments/calendar"))
                            .param("month", "13")
                            .param("year", "2026"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors[0].field").value("month"));

            verifyNoInteractions(appointmentService);
        }

        @Test
        void shouldPassMonthYearAndFiltersWhenGettingCalendar() throws Exception {
            when(appointmentService.getCalendar(anyInt(), anyInt(), any(), any()))
                    .thenReturn(Map.of());

            mvc.perform(as(ADMIN_TOKEN, get("/api/appointments/calendar"))
                            .param("month", "11")
                            .param("year", "2026")
                            .param("doctorId", "10"))
                    .andExpect(status().isOk());

            verify(appointmentService).getCalendar(11, 2026, 10L, null);
        }

        @Test
        void shouldReturnMethodNotAllowedForUnsupportedMethod() throws Exception {
            mvc.perform(as(ADMIN_TOKEN, post("/api/appointments/30/reschedule"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"startTime\":\"2026-11-10T09:00:00\"}"))
                    .andExpect(status().isMethodNotAllowed());
        }
    }
}
