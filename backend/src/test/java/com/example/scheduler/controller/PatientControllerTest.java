package com.example.scheduler.controller;

import com.example.scheduler.dto.patient.PatientResponse;
import com.example.scheduler.dto.personal.PersonalResponse;
import com.example.scheduler.exception.ForbiddenException;
import com.example.scheduler.repository.PatientRepository;
import com.example.scheduler.repository.PersonalRepository;
import com.example.scheduler.security.JwtUtil;
import com.example.scheduler.service.PatientService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.hamcrest.Matchers.hasItem;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(value = PatientController.class, properties = {SliceSecurityConfig.SECRET, SliceSecurityConfig.PUBLIC_PATHS})
@Import(SliceSecurityConfig.class)
@DisplayName("PatientController GET /{id}/doctors: B5 IDOR, B7 inactive token")
class PatientControllerTest {
    private static final long OWN_ID = 1L;
    private static final long OTHER_ID = 2L;

    @Autowired MockMvc mvc;
    @Autowired JwtUtil jwtUtil;
    @MockitoBean PatientService patientService;
    @MockitoBean PatientRepository patientRepository;
    @MockitoBean PersonalRepository personalRepository;

    @BeforeEach
    void activeUsersAndDoctors() {
        when(personalRepository.existsByIdAndActiveTrue(anyLong())).thenReturn(true);
        when(patientRepository.existsByIdAndActiveTrue(anyLong())).thenReturn(true);
        when(patientService.getDoctorsOfPatient(anyLong(), anyLong(), anyString()))
                .thenReturn(List.of(new PersonalResponse(5L, "Dr. Ana", "ana@test.com", true, "DOCTOR", null)));
    }

    @Test
    @DisplayName("B5: service rejects a PATIENT asking for another patient's doctors -> 403 JSON")
    void getDoctors_should_return403_when_serviceForbids() throws Exception {
        when(patientService.getDoctorsOfPatient(OTHER_ID, OWN_ID, "PATIENT"))
                .thenThrow(new ForbiddenException("Not authorize to do this"));

        getAs(OWN_ID, "PATIENT", OTHER_ID)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    @DisplayName("B5: PATIENT -> delegates with caller id and role from the token")
    void getDoctors_should_return200_when_patientAsksOwnId() throws Exception {
        getAs(OWN_ID, "PATIENT", OWN_ID)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Dr. Ana"));
        verify(patientService).getDoctorsOfPatient(OWN_ID, OWN_ID, "PATIENT");
    }

    @Test
    @DisplayName("B5: ASSISTANT asking for any patient -> 200")
    void getDoctors_should_return200_when_assistant() throws Exception {
        getAs(OWN_ID, "ASSISTANT", OTHER_ID).andExpect(status().isOk());
        verify(patientService).getDoctorsOfPatient(OTHER_ID, OWN_ID, "ASSISTANT");
    }

    @Test
    @DisplayName("DOCTOR -> 403 (not in @PreAuthorize)")
    void getDoctors_should_return403_when_doctor() throws Exception {
        getAs(OWN_ID, "DOCTOR", OTHER_ID).andExpect(status().isForbidden());
        verifyNoInteractions(patientService);
    }

    @Test
    @DisplayName("no token -> 401")
    void getDoctors_should_return401_when_noToken() throws Exception {
        mvc.perform(get("/api/patients/{id}/doctors", OWN_ID)).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("B7: token of a deactivated patient -> 401, checked against the patients table")
    void getDoctors_should_return401_when_patientDeactivated() throws Exception {
        when(patientRepository.existsByIdAndActiveTrue(OWN_ID)).thenReturn(false);

        getAs(OWN_ID, "PATIENT", OWN_ID).andExpect(status().isUnauthorized());
        verifyNoInteractions(patientService);
    }

    @Test
    @DisplayName("B7: token of a deactivated assistant -> 401, checked against the personal table")
    void getDoctors_should_return401_when_personalDeactivated() throws Exception {
        when(personalRepository.existsByIdAndActiveTrue(OWN_ID)).thenReturn(false);

        getAs(OWN_ID, "ASSISTANT", OTHER_ID).andExpect(status().isUnauthorized());
        verifyNoInteractions(patientService);
    }

    private ResultActions getAs(long userId, String role, long patientId) throws Exception {
        return mvc.perform(get("/api/patients/{id}/doctors", patientId)
                .header("Authorization", "Bearer " + jwtUtil.generate(userId, role, 1L, "Test")));
    }

    @ParameterizedTest(name = "body {0} -> 400 on email")
    @ValueSource(strings = {
            "{\"name\":\"John Smith\",\"email\":\"not-an-email\"}",
            "{\"name\":\"John Smith\"}"})
    @DisplayName("B14: PUT /update with invalid or missing email -> 400 field error, service never called")
    void update_should_return400_when_emailInvalid(String body) throws Exception {
        mvc.perform(put("/api/patients/update")
                        .header("Authorization", "Bearer " + jwtUtil.generate(OWN_ID, "PATIENT", 1L, "Test"))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[*].field").value(hasItem("email")));
        verify(patientService, never()).updatePatientById(anyLong(), any());
    }

    @ParameterizedTest(name = "body {0} -> 400 on email")
    @ValueSource(strings = {
            "{\"name\":\"John Smith\",\"email\":\"not-an-email\"}",
            "{\"name\":\"John Smith\"}"})
    @DisplayName("B14: PUT /update/{id} (staff) with invalid or missing email -> 400 field error, service never called")
    void updateById_should_return400_when_emailInvalid(String body) throws Exception {
        mvc.perform(put("/api/patients/update/{id}", OTHER_ID)
                        .header("Authorization", "Bearer " + jwtUtil.generate(OWN_ID, "ASSISTANT", 1L, "Test"))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[*].field").value(hasItem("email")));
        verify(patientService, never()).updatePatientById(anyLong(), any());
    }

    @Test
    @DisplayName("B20: POST create patient -> 201")
    void create_should_return201() throws Exception {
        when(patientService.createPatient(any())).thenReturn(new PatientResponse(9L, "Ana", "ana@test.com", null, true));

        mvc.perform(post("/api/patients")
                        .header("Authorization", "Bearer " + jwtUtil.generate(OWN_ID, "ASSISTANT", 1L, "Test"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Ana\",\"email\":\"ana@test.com\",\"ci\":\"123\",\"password\":\"password1\"}"))
                .andExpect(status().isCreated());
    }
}
