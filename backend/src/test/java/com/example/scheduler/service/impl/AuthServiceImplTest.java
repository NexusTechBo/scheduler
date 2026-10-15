package com.example.scheduler.service.impl;

import com.example.scheduler.dto.login.LoginRequest;
import com.example.scheduler.entity.Account;
import com.example.scheduler.entity.Patient;
import com.example.scheduler.entity.Personal;
import com.example.scheduler.entity.Role;
import com.example.scheduler.enums.ERole;
import com.example.scheduler.exception.UnauthorizedException;
import com.example.scheduler.repository.ClinicRepository;
import com.example.scheduler.repository.PatientRepository;
import com.example.scheduler.repository.PersonalRepository;
import com.example.scheduler.security.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthServiceImpl.login: B7 deactivated users")
class AuthServiceImplTest {
    private static final String EMAIL = "qa.pac1@test.com";
    private static final LoginRequest REQUEST = new LoginRequest(EMAIL, "secret", 1L);

    @Mock ClinicRepository clinicRepository;
    @Mock PatientRepository patientRepository;
    @Mock PersonalRepository personalRepository;
    @Mock PasswordEncoder passwordEncoder;
    @Mock JwtUtil jwtUtil;
    @InjectMocks AuthServiceImpl service;

    @BeforeEach
    void noMatchByDefault() {
        lenient().when(personalRepository.findByAccountEmail(EMAIL)).thenReturn(Optional.empty());
        lenient().when(patientRepository.findByAccountEmail(EMAIL)).thenReturn(Optional.empty());
        lenient().when(passwordEncoder.matches(any(), any())).thenReturn(true);
        lenient().when(clinicRepository.existsById(1L)).thenReturn(true);
    }

    @Test
    @DisplayName("B13: unknown clinicId -> 401 Invalid clinic without touching the (missing) tenant schema")
    void login_should_reject_when_clinicUnknown() {
        var request = new LoginRequest(EMAIL, "secret", 999L);

        assertThatThrownBy(() -> service.login(request))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage("Invalid clinic");
        verifyNoInteractions(personalRepository, patientRepository, jwtUtil);
    }

    @Test
    @DisplayName("deactivated patient with correct password -> 401 Invalid Credentials, no token")
    void login_should_reject_when_patientInactive() {
        when(patientRepository.findByAccountEmail(EMAIL)).thenReturn(Optional.of(patient(3L, false)));

        assertThatThrownBy(() -> service.login(REQUEST))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage("Invalid Credentials");
        verifyNoInteractions(jwtUtil);
    }

    @Test
    @DisplayName("deactivated personal with correct password -> 401 Invalid Credentials, no token")
    void login_should_reject_when_personalInactive() {
        when(personalRepository.findByAccountEmail(EMAIL)).thenReturn(Optional.of(personal(4L, false)));

        assertThatThrownBy(() -> service.login(REQUEST))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage("Invalid Credentials");
        verifyNoInteractions(jwtUtil);
    }

    @Test
    @DisplayName("active patient -> token issued with PATIENT role")
    void login_should_issueToken_when_patientActive() {
        when(patientRepository.findByAccountEmail(EMAIL)).thenReturn(Optional.of(patient(3L, true)));

        service.login(REQUEST);

        verify(jwtUtil).generate(3L, "PATIENT", 1L, "Test");
    }

    @Test
    @DisplayName("inactive personal + active patient on the same account -> logs in as patient")
    void login_should_fallBackToPatient_when_personalInactive() {
        when(personalRepository.findByAccountEmail(EMAIL)).thenReturn(Optional.of(personal(4L, false)));
        when(patientRepository.findByAccountEmail(EMAIL)).thenReturn(Optional.of(patient(3L, true)));

        service.login(REQUEST);

        verify(jwtUtil).generate(3L, "PATIENT", 1L, "Test");
    }

    private static Account account() {
        return Account.builder().email(EMAIL).name("Test").password("hash").build();
    }

    private static Patient patient(long id, boolean active) {
        return Patient.builder().id(id).account(account()).active(active).role(Role.builder().name(ERole.PATIENT).build()).build();
    }

    private static Personal personal(long id, boolean active) {
        return Personal.builder().id(id).account(account()).active(active).role(Role.builder().name(ERole.DOCTOR).build()).build();
    }
}
