package com.example.scheduler.controller;

import static com.example.scheduler.controller.SliceTokens.ADMIN_TOKEN;
import static com.example.scheduler.controller.SliceTokens.GUEST_TOKEN;
import static com.example.scheduler.controller.SliceTokens.as;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.scheduler.config.JacksonConfig;
import com.example.scheduler.config.LocaleConfig;
import com.example.scheduler.config.SecurityConfig;
import com.example.scheduler.dto.guest.GuestAppointmentRequest;
import com.example.scheduler.dto.guest.GuestAppointmentResponse;
import com.example.scheduler.dto.guest.GuestNewPatientRequest;
import com.example.scheduler.dto.guest.GuestPatientRequest;
import com.example.scheduler.enums.AppointmentStatus;
import com.example.scheduler.exception.ErrorResponseWriter;
import com.example.scheduler.ratelimiting.RateLimitProperties;
import com.example.scheduler.security.ActiveUserChecker;
import com.example.scheduler.security.jwt.JwtUtil;
import com.example.scheduler.service.GuestService;
import com.example.scheduler.service.SpecialtyService;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = GuestController.class, properties = "rate-limit.enabled=false")
@Import({
    SecurityConfig.class,
    LocaleConfig.class,
    JacksonConfig.class,
    ErrorResponseWriter.class,
    GuestControllerTest.SliceSecurityConfig.class
})
class GuestControllerTest {
    private static final String CI_PATTERN_MESSAGE = "debe contener solo números, entre 5 y 10 dígitos";
    private static final LocalDateTime START = LocalDateTime.of(2026, 11, 10, 9, 0);

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private GuestService guestService;

    @MockitoBean
    private SpecialtyService specialtyService;

    @MockitoBean
    private JwtUtil jwtUtil;

    @MockitoBean
    private ActiveUserChecker activeUserChecker;

    @BeforeEach
    void setUpTokens() {
        SliceTokens.givenStandardTokens(jwtUtil, activeUserChecker);
    }

    private static String existingPatientBody(String ci) {
        return """
                {"doctorId":10,"startTime":"2026-11-10T09:00:00",
                 "patient":{"ci":"%s","phoneNumber":"70000000"}}
                """.formatted(ci);
    }

    private static String newPatientBody(String ci) {
        return """
                {"doctorId":10,"startTime":"2026-11-10T09:00:00",
                 "patient":{"ci":"%s","name":"Ana Perez","phoneNumber":"70000000","email":"ana@mail.test"}}
                """.formatted(ci);
    }

    private static GuestAppointmentResponse response() {
        return new GuestAppointmentResponse(
                99L, "Dr House", "Cardiologia", START, START.plusMinutes(30), AppointmentStatus.PENDING);
    }

    @TestConfiguration
    @EnableConfigurationProperties(RateLimitProperties.class)
    static class SliceSecurityConfig {}

    @Nested
    class BookAppointment {
        @Test
        @DisplayName("should return 400 with patient.ci field error in Spanish when the CI has letters")
        void shouldReturnBadRequestWithCiFieldErrorWhenCiHasLetters() throws Exception {
            mvc.perform(as(GUEST_TOKEN, post("/api/guest/appointments"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(existingPatientBody("12a456")))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400))
                    .andExpect(jsonPath("$.message").value("Error de validación en los datos enviados."))
                    .andExpect(jsonPath("$.errors", hasSize(1)))
                    .andExpect(jsonPath("$.errors[0].field").value("patient.ci"))
                    .andExpect(jsonPath("$.errors[0].message").value(CI_PATTERN_MESSAGE))
                    .andExpect(jsonPath("$.path").value("/api/guest/appointments"));

            verifyNoInteractions(guestService);
        }

        @Test
        void shouldReturnCreatedWhenCiIsValid() throws Exception {
            when(guestService.bookAppointment(any())).thenReturn(response());

            mvc.perform(as(GUEST_TOKEN, post("/api/guest/appointments"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(existingPatientBody("12345")))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.appointmentId").value(99))
                    .andExpect(jsonPath("$.status").value("PENDING"));

            verify(guestService)
                    .bookAppointment(
                            new GuestAppointmentRequest<>(10L, START, new GuestPatientRequest("12345", "70000000")));
        }

        @Test
        void shouldReturnForbiddenWhenStaffTokenIsUsed() throws Exception {
            mvc.perform(as(ADMIN_TOKEN, post("/api/guest/appointments"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(existingPatientBody("12345")))
                    .andExpect(status().isForbidden());

            verifyNoInteractions(guestService);
        }
    }

    @Nested
    class BookAppointmentForNewPatient {
        @Test
        @DisplayName("should return 400 with patient.ci field error in Spanish when the CI has eleven digits")
        void shouldReturnBadRequestWithCiFieldErrorWhenCiHasElevenDigits() throws Exception {
            mvc.perform(as(GUEST_TOKEN, post("/api/guest/appointments/new-patient"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(newPatientBody("12345678901")))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors", hasSize(1)))
                    .andExpect(jsonPath("$.errors[0].field").value("patient.ci"))
                    .andExpect(jsonPath("$.errors[0].message").value(CI_PATTERN_MESSAGE))
                    .andExpect(jsonPath("$.path").value("/api/guest/appointments/new-patient"));

            verifyNoInteractions(guestService);
        }

        @Test
        void shouldReturnCreatedWhenCiIsValid() throws Exception {
            when(guestService.bookAppointmentForNewPatient(any())).thenReturn(response());

            mvc.perform(as(GUEST_TOKEN, post("/api/guest/appointments/new-patient"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(newPatientBody("1234567890")))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.appointmentId").value(99));

            verify(guestService)
                    .bookAppointmentForNewPatient(new GuestAppointmentRequest<>(
                            10L,
                            START,
                            new GuestNewPatientRequest("1234567890", "Ana Perez", "70000000", "ana@mail.test")));
        }
    }
}
