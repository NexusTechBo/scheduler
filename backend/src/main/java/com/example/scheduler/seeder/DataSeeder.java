package com.example.scheduler.seeder;

import com.example.scheduler.config.tenant.TenantContext;
import com.example.scheduler.entity.*;
import com.example.scheduler.enums.ERole;
import com.example.scheduler.repository.*;
import com.example.scheduler.service.SchemaProvisioningService;
import com.example.scheduler.util.Utils;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Component
@RequiredArgsConstructor
public class DataSeeder implements ApplicationRunner {
    private final ClinicRepository clinicRepository;
    private final SpecialtyRepository specialtyRepository;
    private final PersonalRepository personalRepository;
    private final PatientRepository patientRepository;
    private final PasswordEncoder passwordEncoder;
    private final PlatformTransactionManager transactionManager;
    private final SchemaProvisioningService schemaProvisioningService;

    @Override
    public void run(@NonNull ApplicationArguments args) {
        if (clinicRepository.count() > 0) return;

        Clinic downtown = clinicRepository.save(Clinic.builder()
                .name("Downtown Clinic")
                .phoneNumber("+59122441200")
                .slug("downtownclinic")
                .build());
        Clinic uptown = clinicRepository.save(Clinic.builder()
                .name("Uptown Clinic")
                .phoneNumber("+59122441201")
                .slug("uptownclinic")
                .build());

        String pwd = passwordEncoder.encode("password123");

        schemaProvisioningService.createTenantSchema(TenantContext.schemaName(downtown.getSlug()));
        schemaProvisioningService.createTenantSchema(TenantContext.schemaName(uptown.getSlug()));

        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);

        try {
            TenantContext.setCurrentTenant(downtown.getId(), downtown.getSlug());
            transactionTemplate.executeWithoutResult(_ -> seedDowntown(pwd, Utils.usernameSuffix(downtown.getSlug())));
        } finally {
            TenantContext.clear();
        }

        try {
            TenantContext.setCurrentTenant(uptown.getId(), uptown.getSlug());
            transactionTemplate.executeWithoutResult(_ -> seedUptown(pwd, Utils.usernameSuffix(uptown.getSlug())));
        } finally {
            TenantContext.clear();
        }
    }

    private void seedDowntown(String pwd, String usernameSuffix) {

        Specialty gm = specialtyRepository.save(
                Specialty.builder().name("General Medicine").build());
        Specialty dent =
                specialtyRepository.save(Specialty.builder().name("Dentistry").build());
        Specialty peds =
                specialtyRepository.save(Specialty.builder().name("Pediatrics").build());

        personalRepository.save(seedPersonal(
                "Downtown Admin",
                "admin.downtown@clinic.com",
                usernameSuffix,
                pwd,
                "123123121",
                ERole.ADMINISTRATOR,
                null));

        Personal ana = personalRepository.save(seedPersonal(
                "Dr. Ana García", "ana.garcia@clinic.com", usernameSuffix, pwd, "123123122", ERole.DOCTOR, gm));
        personalRepository.save(seedPersonal(
                "Dr. Carlos Méndez", "carlos.mendez@clinic.com", usernameSuffix, pwd, "123123123", ERole.DOCTOR, dent));
        personalRepository.save(seedPersonal(
                "Dr. Laura Torres", "laura.torres@clinic.com", usernameSuffix, pwd, "123123124", ERole.DOCTOR, peds));
        personalRepository.save(seedPersonal(
                "Maria Ramos", "maria.ramos@clinic.com", usernameSuffix, pwd, "123123125", ERole.ASSISTANT, null));

        Patient john =
                patientRepository.save(seedPatient("John Smith", "john.smith@email.com", "+59171000001", "123123126"));
        Patient maria = patientRepository.save(
                seedPatient("María López", "maria.lopez@email.com", "+59171000002", "123123127"));

        ana.getPatients().add(john);
        ana.getPatients().add(maria);
        personalRepository.save(ana);
    }

    private void seedUptown(String pwd, String usernameSuffix) {

        Specialty cardio =
                specialtyRepository.save(Specialty.builder().name("Cardiology").build());
        Specialty derma =
                specialtyRepository.save(Specialty.builder().name("Dermatology").build());
        Specialty trauma = specialtyRepository.save(
                Specialty.builder().name("Traumatology").build());

        personalRepository.save(seedPersonal(
                "Uptown Admin",
                "admin.uptown@clinic.com",
                usernameSuffix,
                pwd,
                "123123128",
                ERole.ADMINISTRATOR,
                null));

        Personal sofia = personalRepository.save(seedPersonal(
                "Dr. Sofía Ramírez",
                "sofia.ramirez@clinic.com",
                usernameSuffix,
                pwd,
                "123123129",
                ERole.DOCTOR,
                cardio));
        personalRepository.save(seedPersonal(
                "Dr. Diego Fernández",
                "diego.fernandez@clinic.com",
                usernameSuffix,
                pwd,
                "123123120",
                ERole.DOCTOR,
                derma));
        personalRepository.save(seedPersonal(
                "Dr. Valentina Cruz",
                "valentina.cruz@clinic.com",
                usernameSuffix,
                pwd,
                "123123111",
                ERole.DOCTOR,
                trauma));
        personalRepository.save(seedPersonal(
                "Pedro Álvarez", "pedro.alvarez@clinic.com", usernameSuffix, pwd, "123123112", ERole.ASSISTANT, null));

        Patient james = patientRepository.save(
                seedPatient("James Wilson", "james.wilson@email.com", "+59171000003", "123123113"));

        sofia.getPatients().add(james);
        personalRepository.save(sofia);
    }

    private static Personal seedPersonal(
            String name,
            String email,
            String usernameSuffix,
            String encodedPassword,
            String ci,
            ERole role,
            Specialty specialty) {
        return Personal.builder()
                .username(email.substring(0, email.indexOf('@')) + usernameSuffix)
                .name(name)
                .email(email)
                .password(encodedPassword)
                .ci(ci)
                .role(role)
                .specialty(specialty)
                .build();
    }

    private static Patient seedPatient(String name, String email, String phoneNumber, String ci) {
        return Patient.builder()
                .name(name)
                .email(email)
                .phoneNumber(phoneNumber)
                .ci(ci)
                .build();
    }
}
