package com.example.scheduler.config;

import com.example.scheduler.config.tenant.TenantContext;
import com.example.scheduler.entity.*;
import com.example.scheduler.enums.ERole;
import com.example.scheduler.repository.*;
import com.example.scheduler.service.SchemaProvisioningService;
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
    private final RoleRepository roleRepository;
    private final PersonalRepository personalRepository;
    private final PatientRepository patientRepository;
    private final AccountRepository accountRepository;
    private final PasswordEncoder passwordEncoder;
    private final PlatformTransactionManager transactionManager;
    private final SchemaProvisioningService schemaProvisioningService;

    @Override
    public void run(@NonNull ApplicationArguments args) {
        if (clinicRepository.count() > 0) return;

        Clinic downtown = clinicRepository.save(Clinic.builder().name("Downtown Clinic").phoneNumber("+1-555-2001").build());
        Clinic uptown = clinicRepository.save(Clinic.builder().name("Uptown Clinic").phoneNumber("+1-555-2002").build());

        String pwd = passwordEncoder.encode("password123");

        Role admin = roleRepository.getByName(ERole.ADMINISTRATOR);
        Role doctor = roleRepository.getByName(ERole.DOCTOR);
        Role assistant = roleRepository.getByName(ERole.ASSISTANT);
        Role patient = roleRepository.getByName(ERole.PATIENT);

        schemaProvisioningService.createTenantSchema("clinic_" + downtown.getId());
        schemaProvisioningService.createTenantSchema("clinic_" + uptown.getId());

        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);

        try {
            TenantContext.setCurrentTenant("clinic_" + downtown.getId());
            transactionTemplate.executeWithoutResult(_ -> seedDowntown(admin, doctor, assistant, patient, pwd));
        } finally {
            TenantContext.clear();
        }

        try {
            TenantContext.setCurrentTenant("clinic_" + uptown.getId());
            transactionTemplate.executeWithoutResult(_ -> seedUptown(admin, doctor, assistant, patient, pwd));
        } finally {
            TenantContext.clear();
        }
    }

    private void seedDowntown(Role admin, Role doctor, Role receptionist, Role patient, String pwd) {
        Specialty gm = specialtyRepository.save(Specialty.builder().name("General Medicine").build());
        Specialty dent = specialtyRepository.save(Specialty.builder().name("Dentistry").build());
        Specialty peds = specialtyRepository.save(Specialty.builder().name("Pediatrics").build());

        personalRepository.save(Personal.builder().account(seedPersonalAccount("Downtown Admin", "admin.downtown@clinic.com", pwd, "123123121")).role(admin).build());

        Personal ana = personalRepository.save(Personal.builder().account(seedPersonalAccount("Dr. Ana García", "ana.garcia@clinic.com", pwd, "123123122")).specialty(gm).role(doctor).build());
        personalRepository.save(Personal.builder().account(seedPersonalAccount("Dr. Carlos Méndez", "carlos.mendez@clinic.com", pwd, "123123123")).specialty(dent).role(doctor).build());
        personalRepository.save(Personal.builder().account(seedPersonalAccount("Dr. Laura Torres", "laura.torres@clinic.com", pwd, "123123124")).specialty(peds).role(doctor).build());
        personalRepository.save(Personal.builder().account(seedPersonalAccount("Maria Ramos", "maria.ramos@clinic.com", pwd, "123123125")).role(receptionist).build());

        Patient john = patientRepository.save(Patient.builder().account(seedPatientAccount("John Smith", "john.smith@email.com", "+1-555-1001", pwd, "123123126")).role(patient).build());
        Patient maria = patientRepository.save(Patient.builder().account(seedPatientAccount("María López", "maria.lopez@email.com", "+1-555-1002", pwd, "123123127")).role(patient).build());

        ana.getPatients().add(john);
        ana.getPatients().add(maria);
        personalRepository.save(ana);
    }

    private void seedUptown(Role admin, Role doctor, Role receptionist, Role patient, String pwd) {
        Specialty cardio = specialtyRepository.save(Specialty.builder().name("Cardiology").build());
        Specialty derma = specialtyRepository.save(Specialty.builder().name("Dermatology").build());
        Specialty trauma = specialtyRepository.save(Specialty.builder().name("Traumatology").build());

        personalRepository.save(Personal.builder().account(seedPersonalAccount("Uptown Admin", "admin.uptown@clinic.com", pwd, "123123128")).role(admin).build());

        Personal sofia = personalRepository.save(Personal.builder().account(seedPersonalAccount("Dr. Sofía Ramírez", "sofia.ramirez@clinic.com", pwd, "123123129")).specialty(cardio).role(doctor).build());
        personalRepository.save(Personal.builder().account(seedPersonalAccount("Dr. Diego Fernández", "diego.fernandez@clinic.com", pwd, "123123120")).specialty(derma).role(doctor).build());
        personalRepository.save(Personal.builder().account(seedPersonalAccount("Dr. Valentina Cruz", "valentina.cruz@clinic.com", pwd, "123123111")).specialty(trauma).role(doctor).build());
        personalRepository.save(Personal.builder().account(seedPersonalAccount("Pedro Álvarez", "pedro.alvarez@clinic.com", pwd, "123123112")).role(receptionist).build());

        Patient james = patientRepository.save(Patient.builder().account(seedPatientAccount("James Wilson", "james.wilson@email.com", "+1-555-1003", pwd, "123123113")).role(patient).build());

        sofia.getPatients().add(james);
        personalRepository.save(sofia);
    }

    private Account seedPersonalAccount(String name, String email, String encodedPassword, String ci) {
        return accountRepository.save(Account.builder().name(name).email(email).password(encodedPassword).ci(ci).build());
    }

    private Account seedPatientAccount(String name, String email, String phoneNumber, String encodedPassword, String ci) {
        return accountRepository.save(Account.builder().name(name).email(email).phoneNumber(phoneNumber).password(encodedPassword).ci(ci).build());
    }
}
