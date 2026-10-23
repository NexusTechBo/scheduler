package com.example.scheduler.service.impl;

import com.example.scheduler.config.tenant.TenantContext;
import com.example.scheduler.dto.clinic.ClinicRequest;
import com.example.scheduler.entity.Clinic;
import com.example.scheduler.entity.Personal;
import com.example.scheduler.enums.ClinicStatus;
import com.example.scheduler.exception.BusinessException;
import com.example.scheduler.repository.ClinicActivationTokenRepository;
import com.example.scheduler.repository.ClinicRepository;
import com.example.scheduler.repository.PersonalRepository;
import com.example.scheduler.service.ClinicActivationService;
import com.example.scheduler.service.SchemaProvisioningService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.SimpleTransactionStatus;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ClinicServiceImplTest {
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 10, 23, 10, 0);
    private static final String SUBDOMAIN = "@clinicasur.com";

    private ClinicRepository clinicRepository;
    private PersonalRepository personalRepository;
    private SchemaProvisioningService schemaProvisioningService;
    private ClinicActivationService clinicActivationService;
    private ClinicActivationTokenRepository tokenRepository;
    private PlatformTransactionManager transactionManager;
    private ClinicServiceImpl service;

    @BeforeEach
    void setUp() {
        clinicRepository = mock(ClinicRepository.class);
        personalRepository = mock(PersonalRepository.class);
        schemaProvisioningService = mock(SchemaProvisioningService.class);
        clinicActivationService = mock(ClinicActivationService.class);
        tokenRepository = mock(ClinicActivationTokenRepository.class);
        var passwordEncoder = mock(PasswordEncoder.class);
        when(passwordEncoder.encode(anyString())).thenReturn("encoded");
        transactionManager = mock(PlatformTransactionManager.class);
        when(transactionManager.getTransaction(any())).thenReturn(new SimpleTransactionStatus());
        var clock = Clock.fixed(NOW.toInstant(ZoneOffset.UTC), ZoneOffset.UTC);
        service = new ClinicServiceImpl(
                clinicRepository,
                personalRepository,
                passwordEncoder,
                schemaProvisioningService,
                transactionManager,
                clinicActivationService,
                tokenRepository,
                clock
        );

        when(clinicRepository.save(any(Clinic.class))).thenAnswer(inv -> {
            Clinic clinic = inv.getArgument(0);
            clinic.setId(10L);
            return clinic;
        });
        when(personalRepository.save(any(Personal.class))).thenAnswer(inv -> {
            Personal personal = inv.getArgument(0);
            personal.setId(1L);
            return personal;
        });
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void createsPendingClinicAndIssuesActivationToken() {
        var response = service.createClinic(request());

        var saved = ArgumentCaptor.forClass(Clinic.class);
        verify(clinicRepository).save(saved.capture());
        assertThat(saved.getValue().getStatus()).isEqualTo(ClinicStatus.PENDING_ACTIVATION);
        verify(schemaProvisioningService).createTenantSchema("clinic_10");
        // The admin gets the activation email instead of the credentials email (no StaffCreatedEvent publisher here).
        verify(clinicActivationService).issueActivationToken(saved.getValue(), "Ana", "ana@mail.com");
        assertThat(response.adminUsername()).isEqualTo("admin" + SUBDOMAIN);
        assertThat(TenantContext.getCurrentTenant()).isNull();
    }

    @Test
    void abandonedPendingClinicsAreRemovedAndRegistrationProceeds() {
        var byName = pendingClinic(7L);
        var bySubdomain = pendingClinic(8L);
        when(clinicRepository.findPendingIdsByNameOrSubdomain("Clínica Sur", SUBDOMAIN)).thenReturn(List.of(7L, 8L));
        when(clinicRepository.findByIdForUpdate(7L)).thenReturn(Optional.of(byName));
        when(clinicRepository.findByIdForUpdate(8L)).thenReturn(Optional.of(bySubdomain));
        when(tokenRepository.existsByClinicId(any())).thenReturn(true);
        when(tokenRepository.existsByClinicIdAndUsedAtIsNullAndExpiresAtAfter(any(), eq(NOW))).thenReturn(false);

        service.createClinic(request());

        verify(clinicRepository).delete(byName);
        verify(clinicRepository).delete(bySubdomain);
        verify(schemaProvisioningService).dropTenantSchema("clinic_7");
        verify(schemaProvisioningService).dropTenantSchema("clinic_8");
        verify(schemaProvisioningService).createTenantSchema("clinic_10");
    }

    @Test
    void pendingClinicWithValidTokenKeepsNameTaken() {
        when(clinicRepository.findPendingIdsByNameOrSubdomain(any(), any())).thenReturn(List.of(7L));
        when(clinicRepository.findByIdForUpdate(7L)).thenReturn(Optional.of(pendingClinic(7L)));
        when(tokenRepository.existsByClinicId(7L)).thenReturn(true);
        when(tokenRepository.existsByClinicIdAndUsedAtIsNullAndExpiresAtAfter(7L, NOW)).thenReturn(true);
        when(clinicRepository.existsByNameIgnoreCase("Clínica Sur")).thenReturn(true);

        assertThatThrownBy(() -> service.createClinic(request()))
                .isInstanceOf(BusinessException.class)
                .hasMessage("A clinic with this name already exists");
        verify(clinicRepository, never()).delete(any());
        verify(schemaProvisioningService, never()).dropTenantSchema(any());
    }

    @Test
    void pendingClinicWithoutTokensIsKeptBecauseItMayBeARegistrationInProgress() {
        when(clinicRepository.findPendingIdsByNameOrSubdomain(any(), any())).thenReturn(List.of(7L));
        when(clinicRepository.findByIdForUpdate(7L)).thenReturn(Optional.of(pendingClinic(7L)));
        when(tokenRepository.existsByClinicId(7L)).thenReturn(false);
        when(clinicRepository.existsBySubdomain(SUBDOMAIN)).thenReturn(true);

        assertThatThrownBy(() -> service.createClinic(request()))
                .isInstanceOf(BusinessException.class)
                .hasMessage("The subdomain " + SUBDOMAIN + " is already taken");
        verify(clinicRepository, never()).delete(any());
        verify(schemaProvisioningService, never()).dropTenantSchema(any());
    }

    @Test
    void failedRemovalIsSkippedAndNormalConflictCheckRuns() {
        var abandoned = pendingClinic(7L);
        when(clinicRepository.findPendingIdsByNameOrSubdomain(any(), any())).thenReturn(List.of(7L));
        when(clinicRepository.findByIdForUpdate(7L)).thenReturn(Optional.of(abandoned));
        when(tokenRepository.existsByClinicId(7L)).thenReturn(true);
        when(tokenRepository.existsByClinicIdAndUsedAtIsNullAndExpiresAtAfter(7L, NOW)).thenReturn(false);
        doThrow(new IllegalStateException("drop failed")).when(schemaProvisioningService).dropTenantSchema("clinic_7");
        when(clinicRepository.existsByNameIgnoreCase("Clínica Sur")).thenReturn(true);

        assertThatThrownBy(() -> service.createClinic(request()))
                .isInstanceOf(BusinessException.class)
                .hasMessage("A clinic with this name already exists");
        verify(transactionManager).rollback(any());
        verify(schemaProvisioningService, never()).createTenantSchema(any());
    }

    @Test
    void activeClinicKeepsSubdomainTaken() {
        when(clinicRepository.findPendingIdsByNameOrSubdomain(any(), any())).thenReturn(List.of());
        when(clinicRepository.existsBySubdomain(SUBDOMAIN)).thenReturn(true);

        assertThatThrownBy(() -> service.createClinic(request()))
                .isInstanceOf(BusinessException.class)
                .hasMessage("The subdomain " + SUBDOMAIN + " is already taken");
        verify(clinicRepository, never()).delete(any());
        verify(schemaProvisioningService, never()).dropTenantSchema(any());
    }

    @Test
    void clinicActivatedWhileWaitingForLockIsKept() {
        var activated = pendingClinic(7L);
        activated.setStatus(ClinicStatus.ACTIVE);
        when(clinicRepository.findPendingIdsByNameOrSubdomain(any(), any())).thenReturn(List.of(7L));
        when(clinicRepository.findByIdForUpdate(7L)).thenReturn(Optional.of(activated));
        when(clinicRepository.existsByNameIgnoreCase("Clínica Sur")).thenReturn(true);

        assertThatThrownBy(() -> service.createClinic(request())).isInstanceOf(BusinessException.class);
        verify(clinicRepository, never()).delete(any());
        verify(schemaProvisioningService, never()).dropTenantSchema(any());
    }

    private static Clinic pendingClinic(Long id) {
        return Clinic.builder().id(id).name("Clínica Sur").status(ClinicStatus.PENDING_ACTIVATION).build();
    }

    private static ClinicRequest request() {
        return new ClinicRequest("Clínica Sur", "clinicasur", "+59170000000", "Ana", "admin",
                "ana@mail.com", "password123", "1234567");
    }
}
