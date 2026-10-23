package com.example.scheduler.service.impl;

import com.example.scheduler.config.tenant.TenantContext;
import com.example.scheduler.dto.clinic.ClinicCreatedResponse;
import com.example.scheduler.dto.clinic.ClinicRequest;
import com.example.scheduler.dto.clinic.ClinicResponse;
import com.example.scheduler.entity.Clinic;
import com.example.scheduler.entity.Personal;
import com.example.scheduler.enums.ERole;
import com.example.scheduler.exception.BusinessException;
import com.example.scheduler.mapper.ClinicMapper;
import com.example.scheduler.repository.ClinicRepository;
import com.example.scheduler.repository.PersonalRepository;
import com.example.scheduler.service.ClinicService;
import com.example.scheduler.service.SchemaProvisioningService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ClinicServiceImpl implements ClinicService {

    private final ClinicRepository clinicRepository;
    private final ClinicMapper clinicMapper;
    private final PersonalRepository personalRepository;
    private final PasswordEncoder passwordEncoder;
    private final SchemaProvisioningService schemaProvisioningService;
    private final PlatformTransactionManager transactionManager;

    @Override
    public ClinicCreatedResponse createClinic(ClinicRequest request) {
        if (clinicRepository.existsByNameIgnoreCase(request.name().strip()))
            throw new BusinessException("clinic.alreadyExists");
        var subdomain = "@" + request.subdomain() + ".com";
        if (clinicRepository.existsBySubdomain(subdomain))
            throw new BusinessException("clinic.subdomainTaken", subdomain);
        var tx = new TransactionTemplate(transactionManager);
        var clinic = tx.execute(_ ->
            clinicRepository.save(Clinic.builder()
                    .name(request.name())
                    .phoneNumber(request.phoneNumber())
                    .subdomain(subdomain)
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

    @Override
    public List<ClinicResponse> findAllClinics() {
        return clinicMapper.toResponseList(clinicRepository.findAll());
    }
}
