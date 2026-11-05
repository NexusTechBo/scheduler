package com.example.scheduler.controller;

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
import com.example.scheduler.dto.clinic.ClinicCreatedResponse;
import com.example.scheduler.dto.clinic.ClinicRequest;
import com.example.scheduler.exception.ErrorResponseWriter;
import com.example.scheduler.ratelimiting.RateLimitProperties;
import com.example.scheduler.security.ActiveUserChecker;
import com.example.scheduler.security.jwt.JwtUtil;
import com.example.scheduler.service.ClinicActivationService;
import com.example.scheduler.service.ClinicService;
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

@WebMvcTest(controllers = ClinicController.class, properties = "rate-limit.enabled=false")
@Import({
    SecurityConfig.class,
    LocaleConfig.class,
    JacksonConfig.class,
    ErrorResponseWriter.class,
    ClinicControllerTest.SliceSecurityConfig.class
})
class ClinicControllerTest {
    private static final String CI_PATTERN_MESSAGE = "debe contener solo números, entre 5 y 10 dígitos";

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private ClinicService clinicService;

    @MockitoBean
    private ClinicActivationService clinicActivationService;

    @MockitoBean
    private JwtUtil jwtUtil;

    @MockitoBean
    private ActiveUserChecker activeUserChecker;

    private static String body(String adminCi) {
        return """
                {"name":"Clinica Demo","slug":"demo","phoneNumber":"70000000","adminName":"Admin Demo",
                 "adminUsername":"admin","adminEmail":"admin@mail.test","adminPassword":"secret123","adminCi":"%s"}
                """.formatted(adminCi);
    }

    @TestConfiguration
    @EnableConfigurationProperties(RateLimitProperties.class)
    static class SliceSecurityConfig {}

    @Nested
    class CreateClinic {
        @Test
        void shouldReturnBadRequestWithAdminCiFieldErrorWhenAdminCiIsMalformed() throws Exception {
            mvc.perform(post("/api/clinics")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body("1234")))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400))
                    .andExpect(jsonPath("$.message").value("Error de validación en los datos enviados."))
                    .andExpect(jsonPath("$.errors", hasSize(1)))
                    .andExpect(jsonPath("$.errors[0].field").value("adminCi"))
                    .andExpect(jsonPath("$.errors[0].message").value(CI_PATTERN_MESSAGE))
                    .andExpect(jsonPath("$.path").value("/api/clinics"));

            verifyNoInteractions(clinicService);
        }

        @Test
        void shouldReturnCreatedWhenAdminCiIsValid() throws Exception {
            when(clinicService.createClinic(any()))
                    .thenReturn(new ClinicCreatedResponse(
                            7L, "Clinica Demo", "70000000", "demo", 1L, "admin", "admin@mail.test"));

            mvc.perform(post("/api/clinics")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body("12345")))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").value(7))
                    .andExpect(jsonPath("$.slug").value("demo"));

            verify(clinicService)
                    .createClinic(new ClinicRequest(
                            "Clinica Demo",
                            "demo",
                            "70000000",
                            "Admin Demo",
                            "admin",
                            "admin@mail.test",
                            "secret123",
                            "12345"));
        }
    }
}
