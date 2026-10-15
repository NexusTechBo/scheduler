package com.example.scheduler.controller;

import com.example.scheduler.repository.PatientRepository;
import com.example.scheduler.repository.PersonalRepository;
import com.example.scheduler.service.ClinicService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.hasItems;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(value = ClinicController.class, properties = {SliceSecurityConfig.SECRET, "security.public-paths=/api/clinics"})
@Import(SliceSecurityConfig.class)
@DisplayName("ClinicController POST: B19 blank name/phone")
class ClinicControllerTest {
    @Autowired MockMvc mvc;
    @MockitoBean ClinicService clinicService;
    @MockitoBean PatientRepository patientRepository;
    @MockitoBean PersonalRepository personalRepository;

    @Test
    @DisplayName("blank name and phoneNumber -> 400 naming both, nothing created")
    void create_should_return400_when_nameAndPhoneBlank() throws Exception {
        mvc.perform(post("/api/clinics").contentType(MediaType.APPLICATION_JSON).content("""
                        {"name":" ","phoneNumber":"","adminName":"Admin","adminEmail":"admin@test.com",
                         "adminPassword":"password1","adminCi":"123"}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[*].field").value(hasItems("name", "phoneNumber")));
        verifyNoInteractions(clinicService);
    }
}
