package com.example.scheduler.service.impl;

import com.example.scheduler.config.tenant.TenantContext;
import com.example.scheduler.dto.login.LoginRequest;
import com.example.scheduler.entity.Clinic;
import com.example.scheduler.entity.Personal;
import com.example.scheduler.enums.ClinicStatus;
import com.example.scheduler.enums.ERole;
import com.example.scheduler.exception.ApiException;
import com.example.scheduler.mapper.PersonalMapper;
import com.example.scheduler.repository.ClinicRepository;
import com.example.scheduler.repository.PersonalRepository;
import com.example.scheduler.security.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuthServiceImplTest {
    private static final String USERNAME = "admin@clinicasur.com";

    private JwtUtil jwtUtil;
    private AuthServiceImpl service;
    private Clinic clinic;

    @BeforeEach
    void setUp() {
        var clinicRepository = mock(ClinicRepository.class);
        var personalRepository = mock(PersonalRepository.class);
        var passwordEncoder = mock(PasswordEncoder.class);
        jwtUtil = mock(JwtUtil.class);
        service = new AuthServiceImpl(clinicRepository, personalRepository, mock(PersonalMapper.class), passwordEncoder, jwtUtil);

        clinic = Clinic.builder().id(5L).name("Clínica Sur").slug("clinicasur")
                .status(ClinicStatus.PENDING_ACTIVATION).build();
        when(clinicRepository.findBySlug("clinicasur")).thenReturn(Optional.of(clinic));
        when(personalRepository.findByUsername(USERNAME)).thenReturn(Optional.of(Personal.builder()
                .id(1L)
                .username(USERNAME)
                .name("Ana")
                .password("hash")
                .role(ERole.ADMINISTRATOR)
                .build()));
        when(passwordEncoder.matches("password123", "hash")).thenReturn(true);
        when(jwtUtil.generate(anyLong(), anyString(), any(), anyString())).thenReturn("jwt");
    }

    @Test
    void pendingClinicWithCorrectPasswordIsForbidden() {
        assertThatThrownBy(() -> service.login(new LoginRequest(USERNAME, "password123")))
                .isInstanceOf(ApiException.class)
                .hasMessage("auth.clinicNotActivated")
                .extracting(e -> ((ApiException) e).getStatus())
                .isEqualTo(HttpStatus.FORBIDDEN);
        verify(jwtUtil, never()).generate(anyLong(), anyString(), any(), anyString());
        assertThat(TenantContext.getCurrentTenant()).isNull();
    }

    @Test
    void pendingClinicWithWrongPasswordIsUnauthorized() {
        assertThatThrownBy(() -> service.login(new LoginRequest(USERNAME, "wrong")))
                .isInstanceOf(ApiException.class)
                .hasMessage("auth.invalidCredentials")
                .extracting(e -> ((ApiException) e).getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void activeClinicWithCorrectPasswordLogsIn() {
        clinic.setStatus(ClinicStatus.ACTIVE);

        assertThat(service.login(new LoginRequest(USERNAME, "password123")).token()).isEqualTo("jwt");
    }
}
