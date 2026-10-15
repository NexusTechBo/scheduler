package com.example.scheduler.service;

import com.example.scheduler.dto.patient.PatientRegisterRequest;
import com.example.scheduler.dto.patient.PatientRequest;
import com.example.scheduler.dto.patient.PatientResponse;
import com.example.scheduler.dto.personal.PersonalResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface PatientService {
    Page<PatientResponse> findAllPatients(Pageable pageable);
    PatientResponse createPatient(PatientRegisterRequest request);
    PatientResponse findPatientById(Long patientId);
    PatientResponse findPatientByPhoneNumber(String phoneNumber);
    PatientResponse updatePatientById(Long patientId, PatientRequest request);
    void deactivatePatientById(Long patientId);
    List<PersonalResponse> getDoctorsOfPatient(Long patientId);
}
