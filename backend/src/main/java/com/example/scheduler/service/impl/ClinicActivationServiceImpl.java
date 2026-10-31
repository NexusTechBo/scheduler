package com.example.scheduler.service.impl;

import com.example.scheduler.dto.clinic.ActivateClinicRequest;
import com.example.scheduler.dto.clinic.ClinicActivatedResponse;
import com.example.scheduler.dto.clinic.ResendActivationRequest;
import com.example.scheduler.entity.Clinic;
import com.example.scheduler.entity.ClinicActivationToken;
import com.example.scheduler.entity.Personal;
import com.example.scheduler.enums.ClinicStatus;
import com.example.scheduler.enums.ERole;
import com.example.scheduler.exception.BadRequestException;
import com.example.scheduler.exception.MessageKeys;
import com.example.scheduler.notification.event.ClinicActivatedEvent;
import com.example.scheduler.notification.event.ClinicActivationEvent;
import com.example.scheduler.repository.ClinicActivationTokenRepository;
import com.example.scheduler.repository.ClinicRepository;
import com.example.scheduler.repository.PersonalRepository;
import com.example.scheduler.service.ClinicActivationService;
import com.example.scheduler.tenant.TenantContext;
import com.example.scheduler.util.TokenUtils;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

@Slf4j
@Service
@RequiredArgsConstructor
public class ClinicActivationServiceImpl implements ClinicActivationService {
    static final Duration TOKEN_VALIDITY = Duration.ofHours(48);
    static final Duration RESEND_COOLDOWN = Duration.ofSeconds(60);

    private final ClinicRepository clinicRepository;
    private final ClinicActivationTokenRepository tokenRepository;
    private final PersonalRepository personalRepository;
    private final PlatformTransactionManager transactionManager;
    private final ApplicationEventPublisher eventPublisher;
    private final Clock clock;

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void issueActivationToken(Clinic clinic, String adminName, String adminEmail) {
        createActivationToken(clinic, adminName, adminEmail);
    }

    private void createActivationToken(Clinic clinic, String adminName, String adminEmail) {
        var token = TokenUtils.generate();
        var now = LocalDateTime.now(clock);
        var expiresAt = now.plus(TOKEN_VALIDITY);
        tokenRepository.save(ClinicActivationToken.builder()
                .clinic(clinic)
                .tokenHash(TokenUtils.hash(token))
                .email(adminEmail)
                .expiresAt(expiresAt)
                .createdAt(now)
                .build());
        eventPublisher.publishEvent(
                new ClinicActivationEvent(clinic.getId(), clinic.getName(), adminName, adminEmail, token, expiresAt));
    }

    @Override
    public ClinicActivatedResponse activateClinic(ActivateClinicRequest request) {
        var tokenHash = TokenUtils.hash(request.token());
        var clinicId = tokenRepository
                .findClinicIdByTokenHash(tokenHash)
                .orElseThrow(ClinicActivationServiceImpl::invalidLink);
        return inClinicTenant(clinicId, _ -> {
            var clinic =
                    clinicRepository.findByIdForUpdate(clinicId).orElseThrow(ClinicActivationServiceImpl::invalidLink);
            var token = tokenRepository
                    .findByTokenHashForUpdate(tokenHash)
                    .orElseThrow(ClinicActivationServiceImpl::invalidLink);
            var now = LocalDateTime.now(clock);
            if (clinic.getStatus() != ClinicStatus.PENDING_ACTIVATION || !token.isUsable(now)) throw invalidLink();
            var admin = getClinicAdmin(clinicId);
            clinic.setStatus(ClinicStatus.ACTIVE);
            clinic.setActivatedAt(now);
            token.setUsedAt(now);
            tokenRepository.markUnusedAsUsedByClinicId(clinicId, now);
            eventPublisher.publishEvent(new ClinicActivatedEvent(
                    clinicId, clinic.getName(), admin.getName(), token.getEmail(), admin.getUsername()));
            log.info("Clinic {} activated", clinicId);
            return new ClinicActivatedResponse(clinic.getName(), admin.getUsername());
        });
    }

    @Override
    public void resendActivation(ResendActivationRequest request) {
        for (var clinicId : tokenRepository.findPendingClinicIdsByEmail(request.email())) {
            try {
                inClinicTenant(clinicId, _ -> {
                    reissueActivationToken(clinicId);
                    return null;
                });
            } catch (RuntimeException e) {
                log.error(
                        "Activation resend failed for clinic {}: {}",
                        clinicId,
                        e.getClass().getSimpleName());
            }
        }
    }

    private void reissueActivationToken(Long clinicId) {
        var clinic = clinicRepository.findByIdForUpdate(clinicId).orElse(null);
        if (clinic == null || clinic.getStatus() != ClinicStatus.PENDING_ACTIVATION) return;
        var latest = tokenRepository
                .findFirstByClinicIdOrderByCreatedAtDesc(clinicId)
                .orElse(null);
        if (latest == null) return;
        var now = LocalDateTime.now(clock);
        if (latest.getCreatedAt().isAfter(now.minus(RESEND_COOLDOWN))) {
            log.info("Activation resend for clinic {} ignored: cooldown active", clinicId);
            return;
        }
        var admin = getClinicAdmin(clinicId);
        tokenRepository.markUnusedAsUsedByClinicId(clinicId, now);
        createActivationToken(clinic, admin.getName(), latest.getEmail());
        log.info("Activation token re-issued for clinic {}", clinicId);
    }

    private Personal getClinicAdmin(Long clinicId) {
        return personalRepository
                .findFirstByRoleOrderByIdAsc(ERole.ADMINISTRATOR)
                .orElseThrow(() -> new IllegalStateException("Clinic " + clinicId + " has no administrator"));
    }

    private <T> T inClinicTenant(Long clinicId, TransactionCallback<T> action) {
        var slug = clinicRepository.findSlugById(clinicId).orElseThrow(ClinicActivationServiceImpl::invalidLink);
        var previousTenant = TenantContext.current();
        try {
            TenantContext.setCurrentTenant(clinicId, slug);
            return new TransactionTemplate(transactionManager).execute(action);
        } finally {
            TenantContext.restore(previousTenant);
        }
    }

    private static BadRequestException invalidLink() {
        return new BadRequestException(MessageKeys.CLINIC_ACTIVATION_LINK_INVALID);
    }
}
