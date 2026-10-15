package com.example.scheduler.controller;

import com.example.scheduler.dto.ScheduleException.ScheduleExceptionResponse;
import com.example.scheduler.repository.PatientRepository;
import com.example.scheduler.repository.PersonalRepository;
import com.example.scheduler.security.JwtUtil;
import com.example.scheduler.service.ScheduleExceptionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
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

@WebMvcTest(value = ScheduleExceptionController.class, properties = {SliceSecurityConfig.SECRET, SliceSecurityConfig.PUBLIC_PATHS})
@Import(SliceSecurityConfig.class)
@DisplayName("ScheduleExceptionController POST: B11 @Valid, B20 201")
class ScheduleExceptionControllerTest {
    private static final long DOCTOR_ID = 2L;

    @Autowired MockMvc mvc;
    @Autowired JwtUtil jwtUtil;
    @MockitoBean ScheduleExceptionService service;
    @MockitoBean PatientRepository patientRepository;
    @MockitoBean PersonalRepository personalRepository;

    @BeforeEach
    void activeUsers() {
        when(personalRepository.existsByIdAndActiveTrue(anyLong())).thenReturn(true);
    }

    @Test
    @DisplayName("missing date -> 400 field error instead of the DB 409, service never called")
    void add_should_return400_when_dateMissing() throws Exception {
        postAs("""
                {"isFullDayBlock":true}""")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[*].field").value(hasItem("date")));
        verifyNoInteractions(service);
    }

    @Test
    @DisplayName("missing isFullDayBlock -> 400 field error")
    void add_should_return400_when_isFullDayBlockMissing() throws Exception {
        postAs("""
                {"date":"2099-10-27"}""")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[*].field").value(hasItem("isFullDayBlock")));
        verifyNoInteractions(service);
    }

    @Test
    @DisplayName("valid block -> 201 with the caller's id")
    void add_should_return201_when_valid() throws Exception {
        when(service.addDoctorScheduleException(eq(DOCTOR_ID), any()))
                .thenReturn(new ScheduleExceptionResponse(4L, null, null, null, true, null));

        postAs("""
                {"date":"2099-10-27","isFullDayBlock":true}""")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(4));
    }

    private ResultActions postAs(String body) throws Exception {
        return mvc.perform(post("/api/scheduleException")
                .header("Authorization", "Bearer " + jwtUtil.generate(DOCTOR_ID, "DOCTOR", 1L, "Test"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }
}
