package com.example.scheduler.service.impl;

import com.example.scheduler.dto.clinic.ClinicCreatedResponse;
import com.example.scheduler.dto.clinic.ClinicRequest;
import com.example.scheduler.entity.Clinic;
import com.example.scheduler.entity.Personal;
import com.example.scheduler.enums.ClinicStatus;
import com.example.scheduler.enums.ERole;
import com.example.scheduler.exception.BusinessException;
import com.example.scheduler.exception.MessageKeys;
import com.example.scheduler.repository.ClinicActivationTokenRepository;
import com.example.scheduler.repository.ClinicRepository;
import com.example.scheduler.repository.PersonalRepository;
import com.example.scheduler.service.ClinicActivationService;
import com.example.scheduler.service.ClinicService;
import com.example.scheduler.service.SchemaProvisioningService;
import com.example.scheduler.tenant.TenantContext;
import com.example.scheduler.util.Utils;
import java.time.Clock;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Slf4j
@Service
@RequiredArgsConstructor
public class ClinicServiceImpl implements ClinicService {
    private final ClinicRepository clinicRepository;
    private final PersonalRepository personalRepository;
    private final PasswordEncoder passwordEncoder;
    private final SchemaProvisioningService schemaProvisioningService;
    private final PlatformTransactionManager transactionManager;
    private final ClinicActivationService clinicActivationService;
    private final ClinicActivationTokenRepository tokenRepository;
    private final Clock clock;

    @Override
    public ClinicCreatedResponse createClinic(ClinicRequest request) {
        var slug = request.slug();
        var tx = new TransactionTemplate(transactionManager);
        removeAbandonedClinics(request.name(), slug, tx);
        if (clinicRepository.existsByNameIgnoreCase(request.name()))
            throw new BusinessException(MessageKeys.CLINIC_ALREADY_EXISTS);
        if (clinicRepository.existsBySlug(slug)) throw new BusinessException(MessageKeys.CLINIC_SLUG_TAKEN, slug);
        var encodedPassword = passwordEncoder.encode(request.adminPassword());
        var clinic = tx.execute(_ -> clinicRepository.save(Clinic.builder()
                .name(request.name())
                .phoneNumber(Utils.normalizePhoneNumber(request.phoneNumber()))
                .slug(slug)
                .status(ClinicStatus.PENDING_ACTIVATION)
                .build()));
        try {
            schemaProvisioningService.createTenantSchema(TenantContext.schemaName(clinic.getSlug()));
            TenantContext.setCurrentTenant(clinic.getId(), clinic.getSlug());
            return tx.execute(_ -> {
                var admin = personalRepository.save(Personal.builder()
                        .username(request.adminUsername() + Utils.usernameSuffix(clinic.getSlug()))
                        .name(request.adminName())
                        .password(encodedPassword)
                        .email(request.adminEmail())
                        .ci(request.adminCi())
                        .role(ERole.ADMINISTRATOR)
                        .build());
                clinicActivationService.issueActivationToken(clinic, admin.getName(), admin.getEmail());
                return new ClinicCreatedResponse(
                        clinic.getId(),
                        clinic.getName(),
                        clinic.getPhoneNumber(),
                        clinic.getSlug(),
                        admin.getId(),
                        admin.getUsername(),
                        admin.getEmail());
            });
        } catch (RuntimeException e) {
            discardFailedClinic(clinic, tx);
            throw e;
        } finally {
            TenantContext.clear();
        }
    }

    private void discardFailedClinic(Clinic clinic, TransactionTemplate tx) {
        TenantContext.clear();
        try {
            tx.executeWithoutResult(_ -> removeAbandonedClinic(clinic));
            log.info("Failed registration of clinic {} rolled back", clinic.getId());
        } catch (RuntimeException e) {
            log.warn("Could not clean up failed registration of clinic {}", clinic.getId(), e);
        }
    }

    private void removeAbandonedClinics(String name, String slug, TransactionTemplate tx) {
        for (var clinicId : clinicRepository.findPendingIdsByNameOrSlug(name, slug)) {
            try {
                var removed = tx.execute(_ -> clinicRepository
                        .findByIdForUpdate(clinicId)
                        .filter(this::isAbandoned)
                        .map(this::removeAbandonedClinic)
                        .orElse(false));
                if (Boolean.TRUE.equals(removed))
                    log.info("Abandoned pending clinic {} removed to free name/slug", clinicId);
            } catch (RuntimeException e) {
                log.warn("Could not remove abandoned clinic {}", clinicId, e);
            }
        }
    }

    private boolean isAbandoned(Clinic clinic) {
        var clinicId = clinic.getId();
        return clinic.getStatus() == ClinicStatus.PENDING_ACTIVATION
                && tokenRepository.existsByClinicId(clinicId)
                && !tokenRepository.existsByClinicIdAndUsedAtIsNullAndExpiresAtAfter(
                        clinicId, LocalDateTime.now(clock));
    }

    private boolean removeAbandonedClinic(Clinic clinic) {
        clinicRepository.delete(clinic);
        clinicRepository.flush();
        schemaProvisioningService.dropTenantSchema(TenantContext.schemaName(clinic.getSlug()));
        return true;
    }
}
