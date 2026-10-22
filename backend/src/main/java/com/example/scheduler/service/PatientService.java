package com.example.scheduler.service;

import com.example.scheduler.dto.patient.PatientRequest;
import com.example.scheduler.dto.patient.PatientResponse;
import com.example.scheduler.dto.personal.PersonalResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface PatientService {
    Page<PatientResponse> findAllPatients(String search, Boolean isActive, Pageable pageable);
    PatientResponse createPatient(PatientRequest request);
    PatientResponse findPatientById(Long patientId);
    PatientResponse updatePatientById(Long patientId, PatientRequest request);
    void deactivatePatientById(Long patientId);
    PatientResponse activatePatientById(Long patientId);
    List<PersonalResponse> getDoctorsOfPatient(Long patientId);
}
