package com.example.scheduler.service.impl;

import com.example.scheduler.dto.patient.PatientRequest;
import com.example.scheduler.dto.patient.PatientResponse;
import com.example.scheduler.dto.personal.PersonalResponse;
import com.example.scheduler.entity.Patient;
import com.example.scheduler.exception.BadRequestException;
import com.example.scheduler.exception.BusinessException;
import com.example.scheduler.exception.ResourceNotFoundException;
import com.example.scheduler.mapper.PatientMapper;
import com.example.scheduler.mapper.PersonalMapper;
import com.example.scheduler.repository.PatientRepository;
import com.example.scheduler.service.PatientService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
    private final PatientMapper patientMapper;
    private final PersonalMapper personalMapper;

    @Override
    public Page<PatientResponse> findAllPatients(Pageable pageable) {
        return patientRepository.findAll(pageable).map(patientMapper::toResponse);
    }

    @Override
    @Transactional
    public PatientResponse createPatient(PatientRequest request) {
        if (patientRepository.existsByCi(request.ci()))
            throw new BusinessException("This patient already exists");
        if (patientRepository.existsByEmail(request.email()))
            throw new BadRequestException("Patient with email " + request.email() + " already exists");
        if (request.phoneNumber() != null && patientRepository.existsByPhoneNumber(request.phoneNumber()))
            throw new BusinessException("Patient with phone number " + request.phoneNumber() + " already exists");
        return patientMapper.toResponse(patientRepository.save(patientMapper.toEntity(request)));
    }

    @Override
    public PatientResponse findPatientById(Long patientId) {
        return patientMapper.toResponse(getPatientOrThrowById(patientId));
    }

    @Override
    @Transactional
    public PatientResponse updatePatientById(Long patientId, PatientRequest request) {
        var patient = getPatientOrThrowById(patientId);
        if (!patient.getCi().equals(request.ci()) && patientRepository.existsByCi(request.ci()))
            throw new BusinessException("This patient already exists");
        if (!patient.getEmail().equals(request.email()) && patientRepository.existsByEmail(request.email()))
            throw new BadRequestException("Patient with email " + request.email() + " already exists");
        if (request.phoneNumber() != null && !patient.getPhoneNumber().equals(request.phoneNumber()) && patientRepository.existsByPhoneNumber(request.phoneNumber()))
            throw new BusinessException("Patient with phone number " + request.phoneNumber() + " already exists");
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
