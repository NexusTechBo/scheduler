package com.example.scheduler.controller;

import com.example.scheduler.dto.personal.PersonalResponse;
import com.example.scheduler.repository.PatientRepository;
import com.example.scheduler.repository.PersonalRepository;
import com.example.scheduler.security.JwtUtil;
import com.example.scheduler.service.PersonalService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.List;

import static org.hamcrest.Matchers.hasItem;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(value = PersonalController.class, properties = {SliceSecurityConfig.SECRET, SliceSecurityConfig.PUBLIC_PATHS})
@Import(SliceSecurityConfig.class)
@DisplayName("PersonalController: B16 doctors filter, B18 caller passed, B19 None specialty, B20 201")
class PersonalControllerTest {
    private static final String NEW_DOCTOR = """
            {"name":"Ana","email":"ana@test.com","ci":"123","password":"password1","roleId":2,"specialtyId":%d}""";

    @Autowired MockMvc mvc;
    @Autowired JwtUtil jwtUtil;
    @MockitoBean PersonalService service;
    @MockitoBean PatientRepository patientRepository;
    @MockitoBean PersonalRepository personalRepository;

    @BeforeEach
    void activeUsers() {
        when(personalRepository.existsByIdAndActiveTrue(anyLong())).thenReturn(true);
        when(patientRepository.existsByIdAndActiveTrue(anyLong())).thenReturn(true);
    }

    @Test
    @DisplayName("B19: DOCTOR with the 'None' specialty (id 1) -> 400 on specialtyId")
    void create_should_return400_when_doctorWithNoneSpecialty() throws Exception {
        mvc.perform(as(1L, "ADMINISTRATOR", post("/api/personal")).contentType(MediaType.APPLICATION_JSON).content(NEW_DOCTOR.formatted(1)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[*].field").value(hasItem("specialtyId")))
                .andExpect(jsonPath("$.errors[*].message").value(hasItem("The doctor role should have a specialty")));
        verifyNoInteractions(service);
    }

    @Test
    @DisplayName("B20: valid personal -> 201")
    void create_should_return201_when_valid() throws Exception {
        when(service.createPersonal(any())).thenReturn(new PersonalResponse(8L, "Ana", "ana@test.com", true, "DOCTOR", "Cardiology"));

        mvc.perform(as(1L, "ADMINISTRATOR", post("/api/personal")).contentType(MediaType.APPLICATION_JSON).content(NEW_DOCTOR.formatted(2)))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("B16: doctors list passes the caller's role to the service")
    void doctors_should_passRole() throws Exception {
        when(service.findAllDoctors(any(), any(), anyString(), any())).thenReturn(Page.empty());

        mvc.perform(as(3L, "PATIENT", get("/api/personal/doctors").param("isActive", "false"))).andExpect(status().isOk());

        verify(service).findAllDoctors(isNull(), eq(false), eq("PATIENT"), any());
    }

    @Test
    @DisplayName("B18: patients of a doctor -> service gets the caller id and role")
    void patients_should_passCaller() throws Exception {
        when(service.getPatientsOfDoctor(anyLong(), anyLong(), anyString())).thenReturn(List.of());

        mvc.perform(as(2L, "DOCTOR", get("/api/personal/{id}/patients", 3L))).andExpect(status().isOk());

        verify(service).getPatientsOfDoctor(3L, 2L, "DOCTOR");
    }

    private MockHttpServletRequestBuilder as(long userId, String role, MockHttpServletRequestBuilder request) {
        return request.header("Authorization", "Bearer " + jwtUtil.generate(userId, role, 1L, "Test"));
    }
}
