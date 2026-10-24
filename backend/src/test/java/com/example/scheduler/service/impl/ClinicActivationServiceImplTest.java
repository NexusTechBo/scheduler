package com.example.scheduler.service.impl;

import com.example.scheduler.config.tenant.TenantContext;
import com.example.scheduler.dto.clinic.ActivateClinicRequest;
import com.example.scheduler.dto.clinic.ResendActivationRequest;
import com.example.scheduler.entity.Clinic;
import com.example.scheduler.entity.ClinicActivationToken;
import com.example.scheduler.entity.Personal;
import com.example.scheduler.enums.ClinicStatus;
import com.example.scheduler.enums.ERole;
import com.example.scheduler.exception.BadRequestException;
import com.example.scheduler.notification.ClinicActivatedEvent;
import com.example.scheduler.notification.ClinicActivationEvent;
import com.example.scheduler.repository.ClinicActivationTokenRepository;
import com.example.scheduler.repository.ClinicRepository;
import com.example.scheduler.repository.PersonalRepository;
import com.example.scheduler.security.ActivationTokens;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.SimpleTransactionStatus;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ClinicActivationServiceImplTest {
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 10, 23, 10, 0);
    private static final String TOKEN = "plain-token";
    private static final String TOKEN_HASH = ActivationTokens.hash(TOKEN);

    private ClinicRepository clinicRepository;
    private ClinicActivationTokenRepository tokenRepository;
    private ApplicationEventPublisher eventPublisher;
    private ClinicActivationServiceImpl service;
    private Clinic clinic;
    private List<String> tenantsAtTransactionStart;

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @BeforeEach
    void setUp() {
        clinicRepository = mock(ClinicRepository.class);
        tokenRepository = mock(ClinicActivationTokenRepository.class);
        var personalRepository = mock(PersonalRepository.class);
        eventPublisher = mock(ApplicationEventPublisher.class);
        var transactionManager = mock(PlatformTransactionManager.class);
        tenantsAtTransactionStart = new ArrayList<>();
        when(transactionManager.getTransaction(any())).thenAnswer(_ -> {
            tenantsAtTransactionStart.add(TenantContext.getCurrentTenant());
            return new SimpleTransactionStatus();
        });
        var clock = Clock.fixed(NOW.toInstant(ZoneOffset.UTC), ZoneOffset.UTC);
        service = new ClinicActivationServiceImpl(
                clinicRepository, tokenRepository, personalRepository, transactionManager, eventPublisher, clock);

        clinic = Clinic.builder().id(5L).name("Clínica Sur").status(ClinicStatus.PENDING_ACTIVATION).build();
        when(clinicRepository.findByIdForUpdate(5L)).thenReturn(Optional.of(clinic));
        when(personalRepository.findFirstByRoleOrderByIdAsc(ERole.ADMINISTRATOR)).thenReturn(Optional.of(
                Personal.builder().id(1L).name("Ana").username("ana@clinicasur.com").role(ERole.ADMINISTRATOR).build()));
    }

    @Test
    void issueStoresOnlyHashWith48hExpiryAndPublishesPlainToken() {
        service.issueActivationToken(clinic, "Ana", "ana@mail.com");

        var saved = ArgumentCaptor.forClass(ClinicActivationToken.class);
        verify(tokenRepository).save(saved.capture());
        var event = captureEvent(ClinicActivationEvent.class);
        assertThat(saved.getValue().getTokenHash())
                .isEqualTo(ActivationTokens.hash(event.token()))
                .isNotEqualTo(event.token());
        assertThat(saved.getValue().getExpiresAt()).isEqualTo(NOW.plusHours(48)).isEqualTo(event.expiresAt());
        assertThat(saved.getValue().getCreatedAt()).isEqualTo(NOW);
        assertThat(saved.getValue().getEmail()).isEqualTo("ana@mail.com").isEqualTo(event.adminEmail());
    }

    @Test
    void validTokenActivatesClinicAndPublishesWelcome() {
        givenToken(token(null, NOW.plusHours(1)));

        var response = service.activateClinic(new ActivateClinicRequest(TOKEN));

        assertThat(response.clinicName()).isEqualTo("Clínica Sur");
        assertThat(response.adminUsername()).isEqualTo("ana@clinicasur.com");
        assertThat(clinic.getStatus()).isEqualTo(ClinicStatus.ACTIVE);
        assertThat(clinic.getActivatedAt()).isEqualTo(NOW);
        verify(tokenRepository).markUnusedAsUsedByClinicId(5L, NOW);
        assertThat(captureEvent(ClinicActivatedEvent.class).adminEmail()).isEqualTo("ana@mail.com");
        assertThat(tenantsAtTransactionStart).containsExactly("clinic_5");
        assertThat(TenantContext.getCurrentTenant()).isNull();
    }

    @Test
    void activationRestoresPreviouslyBoundTenant() {
        TenantContext.setCurrentTenant("clinic_99");
        givenToken(token(null, NOW.plusHours(1)));

        service.activateClinic(new ActivateClinicRequest(TOKEN));

        assertThat(tenantsAtTransactionStart).containsExactly("clinic_5");
        assertThat(TenantContext.getCurrentTenant()).isEqualTo("clinic_99");
    }

    @Test
    void resendContinuesWhenOneClinicFails() {
        when(tokenRepository.findPendingClinicIdsByEmail("ana@mail.com")).thenReturn(List.of(4L, 5L));
        when(clinicRepository.findByIdForUpdate(4L)).thenThrow(new IllegalStateException("db down"));
        var latest = token(null, NOW.plusHours(47));
        latest.setCreatedAt(NOW.minusSeconds(61));
        when(tokenRepository.findFirstByClinicIdOrderByCreatedAtDesc(5L)).thenReturn(Optional.of(latest));

        service.resendActivation(new ResendActivationRequest("ana@mail.com"));

        assertThat(tenantsAtTransactionStart).containsExactly("clinic_4", "clinic_5");
        verify(tokenRepository).save(any(ClinicActivationToken.class));
        assertThat(captureEvent(ClinicActivationEvent.class).clinicId()).isEqualTo(5L);
        assertThat(TenantContext.getCurrentTenant()).isNull();
    }

    @Test
    void unknownTokenIsRejected() {
        when(tokenRepository.findClinicIdByTokenHash(any())).thenReturn(Optional.empty());

        assertInvalidLink();
    }

    @Test
    void usedTokenIsRejected() {
        givenToken(token(NOW.minusMinutes(1), NOW.plusHours(1)));

        assertInvalidLink();
    }

    @Test
    void expiredTokenIsRejected() {
        givenToken(token(null, NOW));

        assertInvalidLink();
    }

    @Test
    void alreadyActiveClinicIsRejected() {
        clinic.setStatus(ClinicStatus.ACTIVE);
        givenToken(token(null, NOW.plusHours(1)));

        assertInvalidLink();
    }

    @Test
    void resendWithinCooldownDoesNothing() {
        when(tokenRepository.findPendingClinicIdsByEmail("ana@mail.com")).thenReturn(List.of(5L));
        var latest = token(null, NOW.plusHours(48));
        latest.setCreatedAt(NOW.minusSeconds(30));
        when(tokenRepository.findFirstByClinicIdOrderByCreatedAtDesc(5L)).thenReturn(Optional.of(latest));

        service.resendActivation(new ResendActivationRequest("ana@mail.com"));

        verify(tokenRepository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any(Object.class));
    }

    @Test
    void resendAfterCooldownInvalidatesPreviousAndIssuesNewToken() {
        when(tokenRepository.findPendingClinicIdsByEmail("ana@mail.com")).thenReturn(List.of(5L));
        var latest = token(null, NOW.plusHours(47));
        latest.setCreatedAt(NOW.minusSeconds(61));
        when(tokenRepository.findFirstByClinicIdOrderByCreatedAtDesc(5L)).thenReturn(Optional.of(latest));

        service.resendActivation(new ResendActivationRequest("ana@mail.com"));

        verify(tokenRepository).markUnusedAsUsedByClinicId(5L, NOW);
        verify(tokenRepository).save(any(ClinicActivationToken.class));
        assertThat(captureEvent(ClinicActivationEvent.class).adminEmail()).isEqualTo("ana@mail.com");
    }

    @Test
    void resendForUnknownEmailDoesNothing() {
        when(tokenRepository.findPendingClinicIdsByEmail(any())).thenReturn(List.of());

        service.resendActivation(new ResendActivationRequest("nobody@mail.com"));

        verify(clinicRepository, never()).findByIdForUpdate(anyLong());
        verify(eventPublisher, never()).publishEvent(any(Object.class));
    }

    @Test
    void tokenUsabilityDependsOnUseAndExpiry() {
        assertThat(token(null, NOW.plusSeconds(1)).isUsable(NOW)).isTrue();
        assertThat(token(null, NOW).isUsable(NOW)).isFalse();
        assertThat(token(NOW.minusSeconds(1), NOW.plusHours(1)).isUsable(NOW)).isFalse();
    }

    private void assertInvalidLink() {
        assertThatThrownBy(() -> service.activateClinic(new ActivateClinicRequest(TOKEN)))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("clinic.activationLinkInvalid");
        verify(eventPublisher, never()).publishEvent(any(Object.class));
        assertThat(TenantContext.getCurrentTenant()).isNull();
    }

    private void givenToken(ClinicActivationToken token) {
        when(tokenRepository.findClinicIdByTokenHash(TOKEN_HASH)).thenReturn(Optional.of(5L));
        when(tokenRepository.findByTokenHashForUpdate(TOKEN_HASH)).thenReturn(Optional.of(token));
    }

    private ClinicActivationToken token(LocalDateTime usedAt, LocalDateTime expiresAt) {
        return ClinicActivationToken.builder()
                .id(10L)
                .clinic(clinic)
                .tokenHash(TOKEN_HASH)
                .email("ana@mail.com")
                .expiresAt(expiresAt)
                .usedAt(usedAt)
                .createdAt(expiresAt.minusHours(48))
                .build();
    }

    private <T> T captureEvent(Class<T> type) {
        var captor = ArgumentCaptor.forClass(type);
        verify(eventPublisher).publishEvent(captor.capture());
        return captor.getValue();
    }
}
