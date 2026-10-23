package com.example.scheduler.service.impl;

import com.example.scheduler.config.tenant.TenantContext;
import com.example.scheduler.dto.clinic.ClinicCreatedResponse;
import com.example.scheduler.dto.clinic.ClinicRequest;
import com.example.scheduler.entity.Clinic;
import com.example.scheduler.entity.Personal;
import com.example.scheduler.enums.ClinicStatus;
import com.example.scheduler.enums.ERole;
import com.example.scheduler.exception.BusinessException;
import com.example.scheduler.repository.ClinicActivationTokenRepository;
import com.example.scheduler.repository.ClinicRepository;
import com.example.scheduler.repository.PersonalRepository;
import com.example.scheduler.service.ClinicActivationService;
import com.example.scheduler.service.ClinicService;
import com.example.scheduler.service.SchemaProvisioningService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;
import java.time.LocalDateTime;

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
        var subdomain = "@" + request.subdomain() + ".com";
        var tx = new TransactionTemplate(transactionManager);
        removeAbandonedClinics(request.name().strip(), subdomain, tx);
        if (clinicRepository.existsByNameIgnoreCase(request.name().strip()))
            throw new BusinessException("A clinic with this name already exists");
        if (clinicRepository.existsBySubdomain(subdomain))
            throw new BusinessException("The subdomain " + subdomain + " is already taken");
        var clinic = tx.execute(_ ->
            clinicRepository.save(Clinic.builder()
                    .name(request.name())
                    .phoneNumber(request.phoneNumber())
                    .subdomain(subdomain)
                    .status(ClinicStatus.PENDING_ACTIVATION)
                    .build())
        );
        var schemaName = "clinic_" + clinic.getId();
        schemaProvisioningService.createTenantSchema(schemaName);
        try {
            TenantContext.setCurrentTenant(schemaName);
            return tx.execute(_ -> {
                var admin = personalRepository.save(Personal.builder()
                        .username(request.adminUsername() + subdomain)
                        .name(request.adminName())
                        .password(passwordEncoder.encode(request.adminPassword()))
                        .email(request.adminEmail())
                        .ci(request.adminCi())
                        .role(ERole.ADMINISTRATOR)
                        .build());
                clinicActivationService.issueActivationToken(clinic, admin.getName(), admin.getEmail());
                return new ClinicCreatedResponse(
                        clinic.getId(),
                        clinic.getName(),
                        clinic.getPhoneNumber(),
                        clinic.getSubdomain(),
                        admin.getId(),
                        admin.getUsername(),
                        admin.getEmail());
            });
        } finally {
            TenantContext.clear();
        }
    }

    /**
     * Frees the name/subdomain held by abandoned pending clinics: PENDING_ACTIVATION with at least one token and
     * none of them usable. A pending clinic without tokens is never removed, because that is also the state of a
     * registration still in progress (clinic row committed, schema being provisioned, admin + token not yet saved).
     * <p>
     * The clinic row is locked while deciding so a concurrent activation either wins (clinic becomes ACTIVE and is kept)
     * or finds the clinic gone. Name and subdomain may belong to two different clinics. Each clinic is handled on its
     * own: an abandoned clinic matching the name is removed even if the request then gets a 409 on the subdomain
     * (intended, it was abandoned anyway). A failed removal is logged and skipped so the normal 409 checks still run.
     */
    private void removeAbandonedClinics(String name, String subdomain, TransactionTemplate tx) {
        for (var clinicId : clinicRepository.findPendingIdsByNameOrSubdomain(name, subdomain)) {
            try {
                var removed = tx.execute(_ -> clinicRepository.findByIdForUpdate(clinicId)
                        .filter(this::isAbandoned)
                        .map(this::removeAbandonedClinic)
                        .orElse(false));
                if (Boolean.TRUE.equals(removed))
                    log.info("Abandoned pending clinic {} removed to free name/subdomain", clinicId);
            } catch (RuntimeException e) {
                log.warn("Could not remove abandoned clinic {}", clinicId, e);
            }
        }
    }

    /** Must be evaluated while holding the clinic row lock. */
    private boolean isAbandoned(Clinic clinic) {
        var clinicId = clinic.getId();
        return clinic.getStatus() == ClinicStatus.PENDING_ACTIVATION
                && tokenRepository.existsByClinicId(clinicId)
                && !tokenRepository.existsByClinicIdAndUsedAtIsNullAndExpiresAtAfter(clinicId, LocalDateTime.now(clock));
    }

    /** Row delete (tokens cascade in the DB) and schema drop share the transaction, so a failure rolls back both. */
    private boolean removeAbandonedClinic(Clinic clinic) {
        clinicRepository.delete(clinic);
        clinicRepository.flush();
        schemaProvisioningService.dropTenantSchema("clinic_" + clinic.getId());
        return true;
    }
}
