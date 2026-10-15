package com.example.scheduler.controller;

import com.example.scheduler.dto.DoctorAvailability.DoctorAvailabilityResponse;
import com.example.scheduler.exception.BusinessException;
import com.example.scheduler.repository.PatientRepository;
import com.example.scheduler.repository.PersonalRepository;
import com.example.scheduler.security.JwtUtil;
import com.example.scheduler.service.DoctorAvailabilityService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import static org.hamcrest.Matchers.hasItem;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(value = DoctorAvailabilityController.class, properties = {SliceSecurityConfig.SECRET, SliceSecurityConfig.PUBLIC_PATHS})
@Import(SliceSecurityConfig.class)
@DisplayName("DoctorAvailabilityController: B1 validation, B7 inactive token, B10 overlap mapping")
class DoctorAvailabilityControllerTest {
    private static final long DOCTOR_ID = 2L;
    private static final String VALID = """
            {"dayOfWeek":"MONDAY","startTime":"09:00","endTime":"12:00","slotDurationMinutes":30}""";

    @Autowired MockMvc mvc;
    @Autowired JwtUtil jwtUtil;
    @MockitoBean DoctorAvailabilityService service;
    @MockitoBean PatientRepository patientRepository;
    @MockitoBean PersonalRepository personalRepository;

    @BeforeEach
    void activeUsers() {
        when(personalRepository.existsByIdAndActiveTrue(anyLong())).thenReturn(true);
        when(patientRepository.existsByIdAndActiveTrue(anyLong())).thenReturn(true);
    }

    @Test
    @DisplayName("valid body as DOCTOR -> 200 and service called with the token's id")
    void add_should_return200_when_valid() throws Exception {
        when(service.addDoctorAvailability(eq(DOCTOR_ID), any()))
                .thenReturn(new DoctorAvailabilityResponse(10L, null, null, null, 30, true));

        postAs("DOCTOR", VALID).andExpect(status().isOk()).andExpect(jsonPath("$.id").value(10));
        verify(service).addDoctorAvailability(eq(DOCTOR_ID), any());
    }

    @ParameterizedTest(name = "slotDurationMinutes={0} -> 400")
    @CsvSource({"0", "-30", "4"})
    @DisplayName("B1: slotDurationMinutes below 5 -> 400 with field error, service never called")
    void add_should_return400_when_slotDurationInvalid(int duration) throws Exception {
        var body = """
                {"dayOfWeek":"MONDAY","startTime":"09:00","endTime":"12:00","slotDurationMinutes":%d}""".formatted(duration);

        postAs("DOCTOR", body)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.errors[*].field").value(hasItem("slotDurationMinutes")));
        verifyNoInteractions(service);
    }

    @ParameterizedTest(name = "missing {0} -> 400")
    @CsvSource({"dayOfWeek", "startTime", "endTime", "slotDurationMinutes"})
    @DisplayName("B1: missing required field -> 400 naming that field")
    void add_should_return400_when_fieldMissing(String field) throws Exception {
        var body = VALID.replaceFirst("\"" + field + "\":(\"[^\"]*\"|-?\\d+),?", "").replace(",}", "}");

        postAs("DOCTOR", body)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[*].field").value(hasItem(field)));
        verifyNoInteractions(service);
    }

    @Test
    @DisplayName("B10: overlapping block (BusinessException) -> 409 with message")
    void add_should_return409_when_overlap() throws Exception {
        when(service.addDoctorAvailability(anyLong(), any()))
                .thenThrow(new BusinessException("That availability overlaps an existing block"));

        postAs("DOCTOR", VALID)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("That availability overlaps an existing block"));
    }

    @Test
    @DisplayName("no token -> 401")
    void add_should_return401_when_noToken() throws Exception {
        mvc.perform(post("/api/doctorAvailability").contentType(MediaType.APPLICATION_JSON).content(VALID))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("B7: token of a deactivated doctor -> 401")
    void add_should_return401_when_userDeactivated() throws Exception {
        when(personalRepository.existsByIdAndActiveTrue(DOCTOR_ID)).thenReturn(false);

        postAs("DOCTOR", VALID).andExpect(status().isUnauthorized());
        verifyNoInteractions(service);
    }

    @ParameterizedTest(name = "role {0} -> 403")
    @CsvSource({"PATIENT", "ASSISTANT", "ADMINISTRATOR"})
    @DisplayName("non-DOCTOR roles -> 403")
    void add_should_return403_when_notDoctor(String role) throws Exception {
        postAs(role, VALID).andExpect(status().isForbidden());
        verifyNoInteractions(service);
    }

    private ResultActions postAs(String role, String body) throws Exception {
        return mvc.perform(post("/api/doctorAvailability")
                .header("Authorization", "Bearer " + jwtUtil.generate(DOCTOR_ID, role, 1L, "Test"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }
}
