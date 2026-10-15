package com.example.scheduler.service.impl;

import com.example.scheduler.dto.patient.PatientRegisterRequest;
import com.example.scheduler.dto.patient.PatientRequest;
import com.example.scheduler.dto.patient.PatientResponse;
import com.example.scheduler.dto.personal.PersonalResponse;
import com.example.scheduler.entity.Account;
import com.example.scheduler.entity.Patient;
import com.example.scheduler.enums.ERole;
import com.example.scheduler.exception.BadRequestException;
import com.example.scheduler.exception.BusinessException;
import com.example.scheduler.exception.ResourceNotFoundException;
import com.example.scheduler.mapper.PatientMapper;
import com.example.scheduler.mapper.PersonalMapper;
import com.example.scheduler.repository.AccountRepository;
import com.example.scheduler.repository.PatientRepository;
import com.example.scheduler.repository.RoleRepository;
import com.example.scheduler.service.PatientService;
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
public class PatientServiceImpl implements PatientService {
    private final PatientRepository patientRepository;
    private final RoleRepository  roleRepository;
    private final AccountRepository accountRepository;
    private final PatientMapper patientMapper;
    private final PersonalMapper personalMapper;
    private final PasswordEncoder passwordEncoder;

    @Override
    public Page<PatientResponse> findAllPatients(Pageable pageable) {
        return patientRepository.findAll(pageable).map(patientMapper::toResponse);
    }

    @Override
    @Transactional
    public PatientResponse createPatient(PatientRegisterRequest request) {
        var account = accountRepository.findByCi(request.ci())
                .orElseGet(() -> {
                    if(accountRepository.existsByEmail(request.email()))
                        throw new BadRequestException("Account with email " + request.email() + " is already exists");
                    return Account.builder()
                            .name(request.name())
                            .password(passwordEncoder.encode(request.password()))
                            .email(request.email())
                            .phoneNumber(request.phoneNumber())
                            .ci(request.ci())
                            .build();
                });
        if (account.getId() != null)
            if (patientRepository.existsByAccountCi(request.ci()))
                throw new BusinessException("This user already exists");
        var role = roleRepository.getByName(ERole.PATIENT);
        var patient = Patient.builder()
                .account(account)
                .role(role)
                .build();
        return patientMapper.toResponse(patientRepository.save(patient));
    }

    @Override
    public PatientResponse findPatientById(Long patientId) {
        return patientMapper.toResponse(getPatientOrThrowById(patientId));
    }

    @Override
    public PatientResponse findPatientByPhoneNumber(String phoneNumber) {
        return patientMapper.toResponse(patientRepository.findByAccountPhoneNumber(phoneNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Patient not found with phone number: " + phoneNumber)));
    }

    @Override
    @Transactional
    public PatientResponse updatePatientById(Long patientId, PatientRequest request) {
        var patient = getPatientOrThrowById(patientId);
        if(!patient.getAccount().getEmail().equals(request.email()) && accountRepository.existsByEmail(request.email()))
            throw new BadRequestException("Account with email " + request.email() + " is already exists");
        patientMapper.toEntityUpdated(request, patient);
        return patientMapper.toResponse(patientRepository.save(patient));
    }

    @Override
    @Transactional
    public void deactivatePatientById(Long patientId) {
        var patient = getPatientOrThrowById(patientId);
        patient.setActive(false);
        patientRepository.save(patient);
    }

    @Override
    public List<PersonalResponse> getDoctorsOfPatient(Long patientId) {
        var patient = getPatientDoctorsOrThrowById(patientId);
        return personalMapper.toResponseList(patient.getDoctors());
    }

    private Patient getPatientOrThrowById(Long patientId) {
        return patientRepository.findById(patientId)
                .orElseThrow(() -> new ResourceNotFoundException("Patient not found with id: " + patientId));
    }

    private Patient getPatientDoctorsOrThrowById(Long patientId) {
        return patientRepository.findPatientDoctorsById(patientId)
                .orElseThrow(() -> new ResourceNotFoundException("Patient not found with id: " + patientId));
    }
}
