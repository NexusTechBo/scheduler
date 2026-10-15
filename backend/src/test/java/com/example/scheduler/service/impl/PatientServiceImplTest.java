package com.example.scheduler.service.impl;

import com.example.scheduler.dto.patient.PatientRegisterRequest;
import com.example.scheduler.dto.patient.PatientRequest;
import com.example.scheduler.entity.Account;
import com.example.scheduler.entity.Patient;
import com.example.scheduler.exception.BusinessException;
import com.example.scheduler.exception.ForbiddenException;
import com.example.scheduler.mapper.PatientMapper;
import com.example.scheduler.repository.AccountRepository;
import com.example.scheduler.repository.PatientRepository;
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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PatientServiceImplTest {
    private static final String PHONE = "70000001";

    @Mock PatientRepository patientRepository;
    @Mock AccountRepository accountRepository;
    @Mock PatientMapper patientMapper;
    @InjectMocks PatientServiceImpl service;

    @Test
    @DisplayName("B5: PATIENT asking for another patient's doctors -> 403, nothing loaded")
    void getDoctorsOfPatient_should_throwForbidden_when_patientAsksOtherId() {
        assertThatThrownBy(() -> service.getDoctorsOfPatient(2L, 1L, "PATIENT"))
                .isInstanceOf(ForbiddenException.class)
                .hasMessage("Not authorize to do this");
        verifyNoInteractions(patientRepository);
    }

    @Test
    @DisplayName("B15: new patient with a phone already used -> 409 naming the phone, nothing saved")
    void createPatient_should_throwConflict_when_phoneTaken() {
        when(accountRepository.findByCi("123")).thenReturn(Optional.empty());
        when(accountRepository.existsByPhoneNumber(PHONE)).thenReturn(true);

        assertThatThrownBy(() -> service.createPatient(new PatientRegisterRequest("Ana", "ana@test.com", "123", "password1", PHONE)))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Account with phone number " + PHONE + " already exists");
        verify(patientRepository, never()).save(any());
    }

    @Test
    @DisplayName("B15: update to a phone used by another account -> 409, nothing saved")
    void updatePatient_should_throwConflict_when_phoneTakenByOther() {
        when(patientRepository.findById(1L)).thenReturn(Optional.of(patient("old-phone")));
        when(accountRepository.existsByPhoneNumber(PHONE)).thenReturn(true);

        assertThatThrownBy(() -> service.updatePatientById(1L, new PatientRequest("Ana", "ana@test.com", PHONE)))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Account with phone number " + PHONE + " already exists");
        verify(patientRepository, never()).save(any());
    }

    @Test
    @DisplayName("B15: update keeping the own phone -> no uniqueness check, saved")
    void updatePatient_should_save_when_phoneUnchanged() {
        when(patientRepository.findById(1L)).thenReturn(Optional.of(patient(PHONE)));

        service.updatePatientById(1L, new PatientRequest("Ana", "ana@test.com", PHONE));

        verify(accountRepository, never()).existsByPhoneNumber(anyString());
        verify(patientRepository).save(any());
    }

    @Test
    @DisplayName("blank phone on create -> stored as null, no uniqueness check (accounts.phone_number is unique)")
    void createPatient_should_storeNullPhone_when_blank() {
        when(accountRepository.findByCi("123")).thenReturn(Optional.empty());
        when(patientRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.createPatient(new PatientRegisterRequest("Ana", "ana@test.com", "123", "password1", "  "));

        var saved = ArgumentCaptor.forClass(Patient.class);
        verify(patientRepository).save(saved.capture());
        assertThat(saved.getValue().getAccount().getPhoneNumber()).isNull();
        verify(accountRepository, never()).existsByPhoneNumber(anyString());
    }

    @Test
    @DisplayName("blank phone on update -> mapped as null; padded phone -> stripped before the check")
    void updatePatient_should_normalizePhone() {
        when(patientRepository.findById(1L)).thenReturn(Optional.of(patient("old-phone")));

        service.updatePatientById(1L, new PatientRequest("Ana", "ana@test.com", ""));
        service.updatePatientById(1L, new PatientRequest("Ana", "ana@test.com", " " + PHONE + " "));

        verify(patientMapper).toEntityUpdated(eq(new PatientRequest("Ana", "ana@test.com", null)), any());
        verify(accountRepository).existsByPhoneNumber(PHONE);
        verify(patientMapper).toEntityUpdated(eq(new PatientRequest("Ana", "ana@test.com", PHONE)), any());
    }

    private static Patient patient(String phone) {
        return Patient.builder().id(1L)
                .account(Account.builder().email("ana@test.com").phoneNumber(phone).build())
                .build();
    }
}
