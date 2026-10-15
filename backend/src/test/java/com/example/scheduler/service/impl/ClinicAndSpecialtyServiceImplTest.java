package com.example.scheduler.service.impl;

import com.example.scheduler.dto.clinic.ClinicRequest;
import com.example.scheduler.dto.specialty.SpecialtyRequest;
import com.example.scheduler.entity.Clinic;
import com.example.scheduler.entity.Personal;
import com.example.scheduler.entity.Specialty;
import com.example.scheduler.exception.BadRequestException;
import com.example.scheduler.exception.BusinessException;
import com.example.scheduler.mapper.ClinicMapper;
import com.example.scheduler.mapper.SpecialtyMapper;
import com.example.scheduler.repository.AccountRepository;
import com.example.scheduler.repository.ClinicRepository;
import com.example.scheduler.repository.PersonalRepository;
import com.example.scheduler.repository.SpecialtyRepository;
import com.example.scheduler.service.SchemaProvisioningService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("B19/B20: clinic registration and specialty names")
class ClinicAndSpecialtyServiceImplTest {
    private static final ClinicRequest REQUEST =
            new ClinicRequest(" Downtown Clinic ", "+1-555", "Admin Ana", "admin@test.com", "password1", "123");

    @Mock ClinicRepository clinicRepository;
    @Mock ClinicMapper clinicMapper;
    @Mock AccountRepository accountRepository;
    @Mock PersonalRepository personalRepository;
    @Mock SchemaProvisioningService schemaProvisioningService;
    @Mock SpecialtyRepository specialtyRepository;
    @Mock SpecialtyMapper specialtyMapper;
    @InjectMocks ClinicServiceImpl clinicService;
    @InjectMocks SpecialtyServiceImpl specialtyService;

    @Test
    @DisplayName("clinic name already registered (any case) -> 409 before creating anything")
    void createClinic_should_throwConflict_when_nameTaken() {
        when(clinicRepository.existsByNameIgnoreCase("Downtown Clinic")).thenReturn(true);

        assertThatThrownBy(() -> clinicService.createClinic(REQUEST))
                .isInstanceOf(BusinessException.class)
                .hasMessage("A clinic with this name already exists");
        verify(clinicRepository, never()).save(any());
        verifyNoInteractions(schemaProvisioningService);
    }

    @Test
    @DisplayName("admin email taken by another account -> 400 before the clinic row or schema exist")
    void createClinic_should_throwBadRequest_when_adminEmailTaken() {
        when(accountRepository.findByCi("123")).thenReturn(Optional.empty());
        when(accountRepository.existsByEmail("admin@test.com")).thenReturn(true);

        assertThatThrownBy(() -> clinicService.createClinic(REQUEST))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Account with email admin@test.com already exists");
        verify(clinicRepository, never()).save(any());
        verifyNoInteractions(schemaProvisioningService);
    }

    @Test
    @DisplayName("new admin account gets adminName; clinic name is stored stripped")
    void createClinic_should_useAdminNameAndStripClinicName() {
        when(accountRepository.findByCi("123")).thenReturn(Optional.empty());
        when(clinicMapper.toEntity(REQUEST)).thenReturn(Clinic.builder().name(REQUEST.name()).build());
        when(clinicRepository.save(any())).thenAnswer(inv -> {
            Clinic clinic = inv.getArgument(0);
            clinic.setId(5L);
            return clinic;
        });
        when(personalRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var response = clinicService.createClinic(REQUEST);

        var personal = ArgumentCaptor.forClass(Personal.class);
        verify(personalRepository).save(personal.capture());
        assertThat(personal.getValue().getAccount().getName()).isEqualTo("Admin Ana");
        assertThat(response.name()).isEqualTo("Downtown Clinic");
        verify(schemaProvisioningService).createTenantSchema("clinic_5");
    }

    @Test
    @DisplayName("specialty differing only in case -> 409")
    void createSpecialty_should_throwConflict_when_nameTakenIgnoringCase() {
        when(specialtyRepository.existsByNameIgnoreCase("general medicine")).thenReturn(true);

        assertThatThrownBy(() -> specialtyService.createSpecialty(new SpecialtyRequest("general medicine")))
                .isInstanceOf(BusinessException.class)
                .hasMessage("This specialty already exists");
        verify(specialtyRepository, never()).save(any());
    }

    @Test
    @DisplayName("specialty name is checked and saved stripped")
    void createSpecialty_should_stripName() {
        when(specialtyMapper.toEntity(new SpecialtyRequest("Cardiology"))).thenReturn(new Specialty());

        specialtyService.createSpecialty(new SpecialtyRequest(" Cardiology "));

        verify(specialtyRepository).existsByNameIgnoreCase("Cardiology");
        verify(specialtyMapper).toEntity(new SpecialtyRequest("Cardiology"));
    }
}
