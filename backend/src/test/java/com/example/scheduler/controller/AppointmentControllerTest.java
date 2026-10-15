package com.example.scheduler.controller;

import com.example.scheduler.entity.Appointment;
import com.example.scheduler.repository.PatientRepository;
import com.example.scheduler.repository.PersonalRepository;
import com.example.scheduler.security.JwtUtil;
import com.example.scheduler.service.AppointmentService;
import org.hibernate.query.SemanticException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.data.core.PropertyReferenceException;
import org.springframework.data.core.TypeInformation;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.hasItem;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(value = AppointmentController.class, properties = {SliceSecurityConfig.SECRET, SliceSecurityConfig.PUBLIC_PATHS})
@Import(SliceSecurityConfig.class)
@DisplayName("AppointmentController: B13 bad month and sort -> 400")
class AppointmentControllerTest {
    @Autowired MockMvc mvc;
    @Autowired JwtUtil jwtUtil;
    @MockitoBean AppointmentService service;
    @MockitoBean PatientRepository patientRepository;
    @MockitoBean PersonalRepository personalRepository;

    @BeforeEach
    void activeUsers() {
        when(personalRepository.existsByIdAndActiveTrue(anyLong())).thenReturn(true);
        when(patientRepository.existsByIdAndActiveTrue(anyLong())).thenReturn(true);
    }

    @ParameterizedTest(name = "month={0} year={1} -> 400 on {2}")
    @CsvSource({"0,2026,month", "13,2026,month", "1,1899,year", "1,999999999,year"})
    @DisplayName("calendar month outside 1..12 or year outside 1900..9999 -> 400 naming the param, service never called")
    void calendar_should_return400_when_outOfRange(int month, int year, String field) throws Exception {
        mvc.perform(auth(get("/api/appointments/calendar").param("month", String.valueOf(month)).param("year", String.valueOf(year))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[*].field").value(hasItem(field)));
        verifyNoInteractions(service);
    }

    @Test
    @DisplayName("calendar month 12 -> 200")
    void calendar_should_return200_when_monthValid() throws Exception {
        when(service.getCalendar(anyInt(), anyInt(), any(), any())).thenReturn(Map.of());

        mvc.perform(auth(get("/api/appointments/calendar").param("month", "12").param("year", "2026")))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("sort on an unknown property of a @Query (Hibernate SemanticException) -> 400")
    void list_should_return400_when_sortRejectedByHibernate() throws Exception {
        when(service.findAllAppointments(any(), any(), any(), any())).thenThrow(new InvalidDataAccessApiUsageException(
                "bad sort", new IllegalArgumentException(new SemanticException("Could not resolve attribute 'foo'"))));

        mvc.perform(auth(get("/api/appointments").param("sort", "foo")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid sort property"));
    }

    @Test
    @DisplayName("sort on an unknown property of a derived query (PropertyReferenceException) -> 400")
    void list_should_return400_when_propertyReferenceUnknown() throws Exception {
        when(service.findAllAppointments(any(), any(), any(), any()))
                .thenThrow(new PropertyReferenceException("foo", TypeInformation.of(Appointment.class), List.of()));

        mvc.perform(auth(get("/api/appointments").param("sort", "foo")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid sort property 'foo'"));
    }

    @Test
    @DisplayName("other InvalidDataAccessApiUsageException -> still 500")
    void list_should_return500_when_otherApiMisuse() throws Exception {
        when(service.findAllAppointments(any(), any(), any(), any()))
                .thenThrow(new InvalidDataAccessApiUsageException("programming error"));

        mvc.perform(auth(get("/api/appointments")))
                .andExpect(status().isInternalServerError());
    }

    private MockHttpServletRequestBuilder auth(MockHttpServletRequestBuilder request) {
        return request.header("Authorization", "Bearer " + jwtUtil.generate(1L, "ASSISTANT", 1L, "Test"));
    }
}
