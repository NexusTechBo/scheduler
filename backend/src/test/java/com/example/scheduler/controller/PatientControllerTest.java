package com.example.scheduler.controller;

import static com.example.scheduler.controller.SliceTokens.ASSISTANT_TOKEN;
import static com.example.scheduler.controller.SliceTokens.as;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.scheduler.config.JacksonConfig;
import com.example.scheduler.config.LocaleConfig;
import com.example.scheduler.config.SecurityConfig;
import com.example.scheduler.dto.patient.PatientRequest;
import com.example.scheduler.dto.patient.PatientResponse;
import com.example.scheduler.exception.ErrorResponseWriter;
import com.example.scheduler.ratelimiting.RateLimitProperties;
import com.example.scheduler.security.ActiveUserChecker;
import com.example.scheduler.security.jwt.JwtUtil;
import com.example.scheduler.service.PatientService;
import org.junit.jupiter.api.BeforeEach;
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

@WebMvcTest(controllers = PatientController.class, properties = "rate-limit.enabled=false")
@Import({
    SecurityConfig.class,
    LocaleConfig.class,
    JacksonConfig.class,
    ErrorResponseWriter.class,
    PatientControllerTest.SliceSecurityConfig.class
})
class PatientControllerTest {
    private static final String CI_PATTERN_MESSAGE = "debe contener solo números, entre 5 y 10 dígitos";

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private PatientService patientService;

    @MockitoBean
    private JwtUtil jwtUtil;

    @MockitoBean
    private ActiveUserChecker activeUserChecker;

    @BeforeEach
    void setUpTokens() {
        SliceTokens.givenStandardTokens(jwtUtil, activeUserChecker);
    }

    private static String body(String ci) {
        return """
                {"ci":"%s","name":"Ana Perez","email":"ana@mail.test","phoneNumber":"70000000"}
                """.formatted(ci);
    }

    private static PatientRequest request(String ci) {
        return new PatientRequest(ci, "Ana Perez", "ana@mail.test", "70000000");
    }

    private static PatientResponse response(Long id, String ci) {
        return new PatientResponse(id, ci, "Ana Perez", "ana@mail.test", "70000000", true);
    }

    @TestConfiguration
    @EnableConfigurationProperties(RateLimitProperties.class)
    static class SliceSecurityConfig {}

    @Nested
    class CreatePatient {
        @Test
        void shouldReturnBadRequestWithCiFieldErrorWhenCiIsMalformed() throws Exception {
            mvc.perform(as(ASSISTANT_TOKEN, post("/api/patients"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body("12a456")))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400))
                    .andExpect(jsonPath("$.message").value("Error de validación en los datos enviados."))
                    .andExpect(jsonPath("$.errors", hasSize(1)))
                    .andExpect(jsonPath("$.errors[0].field").value("ci"))
                    .andExpect(jsonPath("$.errors[0].message").value(CI_PATTERN_MESSAGE))
                    .andExpect(jsonPath("$.path").value("/api/patients"));

            verifyNoInteractions(patientService);
        }

        @Test
        void shouldReturnBadRequestWithCiFieldErrorWhenCiHasElevenDigits() throws Exception {
            mvc.perform(as(ASSISTANT_TOKEN, post("/api/patients"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body("12345678901")))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors[0].field").value("ci"))
                    .andExpect(jsonPath("$.errors[0].message").value(CI_PATTERN_MESSAGE));

            verifyNoInteractions(patientService);
        }

        @Test
        void shouldReturnCreatedWhenCiHasFiveDigits() throws Exception {
            when(patientService.createPatient(any())).thenReturn(response(20L, "12345"));

            mvc.perform(as(ASSISTANT_TOKEN, post("/api/patients"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body("12345")))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").value(20))
                    .andExpect(jsonPath("$.ci").value("12345"));

            verify(patientService).createPatient(request("12345"));
        }
    }

    @Nested
    class UpdatePatient {
        @Test
        void shouldReturnBadRequestWithCiFieldErrorWhenCiHasFourDigits() throws Exception {
            mvc.perform(as(ASSISTANT_TOKEN, put("/api/patients/20"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body("1234")))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors", hasSize(1)))
                    .andExpect(jsonPath("$.errors[0].field").value("ci"))
                    .andExpect(jsonPath("$.errors[0].message").value(CI_PATTERN_MESSAGE))
                    .andExpect(jsonPath("$.path").value("/api/patients/20"));

            verifyNoInteractions(patientService);
        }

        @Test
        void shouldReturnOkWhenCiHasTenDigits() throws Exception {
            when(patientService.updatePatientById(anyLong(), any())).thenReturn(response(20L, "1234567890"));

            mvc.perform(as(ASSISTANT_TOKEN, put("/api/patients/20"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body("1234567890")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.ci").value("1234567890"));

            verify(patientService).updatePatientById(20L, request("1234567890"));
        }
    }
}
