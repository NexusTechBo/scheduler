package com.example.scheduler.service.impl;

import com.example.scheduler.dto.personal.AssignAndRemoveRequest;
import com.example.scheduler.dto.personal.PersonalRequest;
import com.example.scheduler.entity.Account;
import com.example.scheduler.entity.Personal;
import com.example.scheduler.entity.Role;
import com.example.scheduler.enums.ERole;
import com.example.scheduler.enums.ESpecialty;
import com.example.scheduler.exception.BadRequestException;
import com.example.scheduler.exception.ForbiddenException;
import com.example.scheduler.repository.PersonalRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PersonalServiceImplTest {
    @Mock PersonalRepository personalRepository;
    @InjectMocks PersonalServiceImpl service;

    @ParameterizedTest(name = "role={0}, ?isActive={1} -> query active={2}")
    @CsvSource({
            "PATIENT,,true",
            "PATIENT,false,true",
            "ASSISTANT,,true",
            "ASSISTANT,false,false",
    })
    @DisplayName("B16: doctors list defaults to active; a PATIENT can't ask for inactive ones")
    void findAllDoctors_should_resolveActiveFilter(String role, Boolean isActive, Boolean expected) {
        when(personalRepository.findAllDoctorsByFilters(any(), any(), any())).thenReturn(Page.empty());

        service.findAllDoctors(null, isActive, role, Pageable.unpaged());

        verify(personalRepository).findAllDoctorsByFilters(null, expected, Pageable.unpaged());
    }

    @Test
    @DisplayName("B18: assign a patient to an assistant -> 400 is not a doctor, nothing saved")
    void assignPatient_should_throwBadRequest_when_targetIsNotDoctor() {
        when(personalRepository.findDoctorPatientsById(5L)).thenReturn(Optional.of(personal(5L, ERole.ASSISTANT)));

        assertThatThrownBy(() -> service.assignPatient(new AssignAndRemoveRequest(1L, 5L), 9L, "ASSISTANT"))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Personal with id 5 is not a doctor");
        verify(personalRepository, never()).save(any());
    }

    @Test
    @DisplayName("B18: remove a patient from an assistant -> 400 is not a doctor")
    void removePatient_should_throwBadRequest_when_targetIsNotDoctor() {
        when(personalRepository.findDoctorPatientsById(5L)).thenReturn(Optional.of(personal(5L, ERole.ASSISTANT)));

        assertThatThrownBy(() -> service.removePatient(new AssignAndRemoveRequest(1L, 5L), 9L, "ASSISTANT"))
                .isInstanceOf(BadRequestException.class);
        verify(personalRepository, never()).save(any());
    }

    @Test
    @DisplayName("B18: DOCTOR listing another doctor's patients -> 403, nothing loaded")
    void getPatientsOfDoctor_should_throwForbidden_when_doctorAsksOtherId() {
        assertThatThrownBy(() -> service.getPatientsOfDoctor(3L, 2L, "DOCTOR"))
                .isInstanceOf(ForbiddenException.class);
        verifyNoInteractions(personalRepository);
    }

    @Test
    @DisplayName("B18: ASSISTANT listing patients of an assistant -> 400 is not a doctor")
    void getPatientsOfDoctor_should_throwBadRequest_when_targetIsNotDoctor() {
        when(personalRepository.findDoctorPatientsById(5L)).thenReturn(Optional.of(personal(5L, ERole.ASSISTANT)));

        assertThatThrownBy(() -> service.getPatientsOfDoctor(5L, 9L, "ASSISTANT"))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Personal with id 5 is not a doctor");
    }

    @Test
    @DisplayName("B18: DOCTOR listing own patients -> loaded")
    void getPatientsOfDoctor_should_load_when_doctorAsksOwnId() {
        when(personalRepository.findDoctorPatientsById(2L)).thenReturn(Optional.of(personal(2L, ERole.DOCTOR)));

        service.getPatientsOfDoctor(2L, 2L, "DOCTOR");

        verify(personalRepository).findDoctorPatientsById(2L);
    }

    @Test
    @DisplayName("B19: update a doctor to the 'None' specialty -> 400, nothing saved")
    void updatePersonal_should_throwBadRequest_when_doctorGetsNoneSpecialty() {
        when(personalRepository.findById(2L)).thenReturn(Optional.of(personal(2L, ERole.DOCTOR)));

        assertThatThrownBy(() -> service.updatePersonalById(2L, new PersonalRequest("Ana", "ana@test.com", ESpecialty.DEFAULT.getId())))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("The doctor role should have a specialty");
        verify(personalRepository, never()).save(any());
    }

    @Test
    @DisplayName("B20: unknown personal id -> 404 'Personal not found'")
    void findPersonalById_should_throwNotFoundWithFixedMessage() {
        when(personalRepository.findById(anyLong())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findPersonalById(42L))
                .hasMessage("Personal not found with id: 42");
    }

    private static Personal personal(long id, ERole role) {
        return Personal.builder().id(id)
                .account(Account.builder().email("ana@test.com").build())
                .role(Role.builder().name(role).build())
                .build();
    }
}
