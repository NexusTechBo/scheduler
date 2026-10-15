package com.example.scheduler.service.impl;

import com.example.scheduler.exception.ForbiddenException;
import com.example.scheduler.repository.PatientRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class PatientServiceImplTest {
    @Mock PatientRepository patientRepository;
    @InjectMocks PatientServiceImpl service;

    @Test
    @DisplayName("B5: PATIENT asking for another patient's doctors -> 403, nothing loaded")
    void getDoctorsOfPatient_should_throwForbidden_when_patientAsksOtherId() {
        assertThatThrownBy(() -> service.getDoctorsOfPatient(2L, 1L, "PATIENT"))
                .isInstanceOf(ForbiddenException.class)
                .hasMessage("Not authorize to do this");
        verifyNoInteractions(patientRepository);
    }
}
