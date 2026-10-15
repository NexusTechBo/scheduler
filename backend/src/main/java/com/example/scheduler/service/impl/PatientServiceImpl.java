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
import com.example.scheduler.exception.ForbiddenException;
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
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Objects;

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
        var phoneNumber = normalizePhoneNumber(request.phoneNumber());
        var account = accountRepository.findByCi(request.ci())
                .orElseGet(() -> {
                    if(accountRepository.existsByEmail(request.email()))
                        throw new BadRequestException("Account with email " + request.email() + " already exists");
                    verifyPhoneNumberFree(phoneNumber);
                    return Account.builder()
                            .name(request.name())
                            .password(passwordEncoder.encode(request.password()))
                            .email(request.email())
                            .phoneNumber(phoneNumber)
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
    @Transactional
    public PatientResponse updatePatientById(Long patientId, PatientRequest request) {
        var patient = getPatientOrThrowById(patientId);
        request = new PatientRequest(request.name(), request.email(), normalizePhoneNumber(request.phoneNumber()));
        if(!patient.getAccount().getEmail().equals(request.email()) && accountRepository.existsByEmail(request.email()))
            throw new BadRequestException("Account with email " + request.email() + " already exists");
        if (!Objects.equals(patient.getAccount().getPhoneNumber(), request.phoneNumber()))
            verifyPhoneNumberFree(request.phoneNumber());
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
    public List<PersonalResponse> getDoctorsOfPatient(Long patientId, Long userId, String role) {
        if (ERole.PATIENT.name().equals(role) && !patientId.equals(userId))
            throw new ForbiddenException("Not authorize to do this");
        var patient = getPatientDoctorsOrThrowById(patientId);
        return personalMapper.toResponseList(patient.getDoctors());
    }

    private static String normalizePhoneNumber(String phoneNumber) {
        return StringUtils.hasText(phoneNumber) ? phoneNumber.strip() : null;
    }

    private void verifyPhoneNumberFree(String phoneNumber) {
        if (phoneNumber != null && accountRepository.existsByPhoneNumber(phoneNumber))
            throw new BusinessException("Account with phone number " + phoneNumber + " already exists");
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
