package com.example.scheduler.service.impl;

import com.example.scheduler.dto.patient.PatientResponse;
import com.example.scheduler.dto.personal.AssignAndRemoveRequest;
import com.example.scheduler.dto.personal.PersonalRegisterRequest;
import com.example.scheduler.dto.personal.PersonalRequest;
import com.example.scheduler.dto.personal.PersonalResponse;
import com.example.scheduler.entity.*;
import com.example.scheduler.enums.ERole;
import com.example.scheduler.exception.BadRequestException;
import com.example.scheduler.exception.BusinessException;
import com.example.scheduler.exception.ForbiddenException;
import com.example.scheduler.exception.ResourceNotFoundException;
import com.example.scheduler.mapper.PatientMapper;
import com.example.scheduler.mapper.PersonalMapper;
import com.example.scheduler.repository.*;
import com.example.scheduler.service.PersonalService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PersonalServiceImpl implements PersonalService {
    private final PersonalRepository personalRepository;
    private final PatientRepository patientRepository;
    private final SpecialtyRepository specialtyRepository;
    private final RoleRepository roleRepository;
    private final AccountRepository accountRepository;
    private final PersonalMapper personalMapper;
    private final PatientMapper patientMapper;
    private final PasswordEncoder passwordEncoder;

    @Override
    public Page<PersonalResponse> findAllDoctors(Long specialtyId, Boolean isActive, Pageable pageable) {
        if(specialtyId != null) getSpecialtyOrThrowById(specialtyId);
        return personalRepository.findAllDoctorsByFilters(specialtyId, isActive, pageable)
                .map(personalMapper::toResponse);
    }

    @Override
    public Page<PersonalResponse> findAllPersonal(Long specialtyId, Boolean isActive, Long roleId, Pageable pageable) {
        if(specialtyId != null) getSpecialtyOrThrowById(specialtyId);
        if(roleId != null) getRoleOrThrowById(roleId);
        return personalRepository.findAllByFilters(specialtyId, isActive, roleId, pageable)
                .map(personalMapper::toResponse);
    }

    @Override
    @Transactional
    public PersonalResponse createPersonal(PersonalRegisterRequest request) {
        var account = accountRepository.findByCi(request.ci())
                .orElseGet(() -> {
                    if(accountRepository.existsByEmail(request.email()))
                        throw new BadRequestException("Account with email " + request.email() + " is already exists");
                    return Account.builder()
                            .name(request.name())
                            .password(passwordEncoder.encode(request.password()))
                            .email(request.email())
                            .ci(request.ci())
                            .build();
                });
        if (account.getId() != null)
            if (personalRepository.existsByAccountCi(request.ci()))
                throw new BusinessException("This user already exists");
        var role = getRoleOrThrowById(request.roleId());
        var specialty = getSpecialtyOrThrowById(request.specialtyId());
        var personal = Personal.builder()
                .account(account)
                .role(role)
                .specialty(specialty)
                .build();
        return personalMapper.toResponse(personalRepository.save(personal));
    }

    @Override
    public PersonalResponse findPersonalById(Long personalId) {
        return personalMapper.toResponse(getPersonalOrThrowById(personalId));
    }

    @Override
    @Transactional
    public PersonalResponse updatePersonalById(Long personalId, PersonalRequest request) {
        var personal = getPersonalOrThrowById(personalId);
        if(!personal.getAccount().getEmail().equals(request.email()) && accountRepository.existsByEmail(request.email())){
            throw new BadRequestException("Account with email " + request.email() + " is already exists");
        }
        personalMapper.toEntityUpdated(request, personal);
        // null keeps the current specialty (assistants and self-updates send none)
        if (request.specialtyId() != null) {
            if (personal.getRole().getName() != ERole.DOCTOR)
                throw new BadRequestException("Only doctors can have a specialty");
            personal.setSpecialty(getSpecialtyOrThrowById(request.specialtyId()));
        }
        return personalMapper.toResponse(personalRepository.save(personal));
    }

    @Override
    @Transactional
    public void deactivatePersonalById(Long personalId) {
        var personal = getPersonalOrThrowById(personalId);
        personal.setActive(false);
        personalRepository.save(personal);
    }

    @Override
    @Transactional
    public void assignPatient(AssignAndRemoveRequest request, Long userId, String role) {
        var doctor = getPersonalPatientsOrThrowById(request.doctorId());
        var patient = getPatientOrThrowById(request.patientId());
        verifyDoctorPermission(role, doctor.getId(), userId);
        if (!doctor.getPatients().contains(patient)) {
            doctor.getPatients().add(patient);
            personalRepository.save(doctor);
        }
    }

    @Override
    @Transactional
    public void removePatient(AssignAndRemoveRequest request, Long userId, String role) {
        var doctor = getPersonalPatientsOrThrowById(request.doctorId());
        var patient = getPatientOrThrowById(request.patientId());
        verifyDoctorPermission(role, doctor.getId(), userId);
        if (doctor.getPatients().contains(patient)) {
            doctor.getPatients().remove(patient);
            personalRepository.save(doctor);
        }
    }

    @Override
    public List<PatientResponse> getPatientsOfDoctor(Long doctorId) {
        var doctor = getPersonalPatientsOrThrowById(doctorId);
        return patientMapper.toResponseList(doctor.getPatients());
    }

    private Personal getPersonalOrThrowById(Long personalId) {
        return personalRepository.findById(personalId)
                .orElseThrow(() -> new ResourceNotFoundException("Personal not fount with id: " + personalId));
    }

    private Personal getPersonalPatientsOrThrowById(Long personalId) {
        return personalRepository.findDoctorPatientsById(personalId)
                .orElseThrow(() -> new ResourceNotFoundException("Personal not fount with id: " + personalId));
    }

    private Specialty getSpecialtyOrThrowById(Long specialtyId) {
        if(specialtyId == null){
            return null;
        }
        return  specialtyRepository.findById(specialtyId)
                .orElseThrow(() -> new ResourceNotFoundException("Specialty not found with id: " + specialtyId));
    }

    private Role getRoleOrThrowById(Long roleId) {
        return roleRepository.findById(roleId)
                .orElseThrow(() -> new ResourceNotFoundException("Role not found with id: " + roleId));
    }

    private Patient getPatientOrThrowById(Long patientId) {
        return patientRepository.findById(patientId)
                .orElseThrow(() -> new ResourceNotFoundException("Patient not found with id: " + patientId));
    }

    public void verifyDoctorPermission(String role, Long accountId, Long userId) {
        if (role.equals(ERole.DOCTOR.name()) && !accountId.equals(userId))
            throw new ForbiddenException("Not authorize to do this");
    }
}
