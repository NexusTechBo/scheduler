package com.example.scheduler.service.impl;

import com.example.scheduler.dto.patient.PatientResponse;
import com.example.scheduler.dto.personal.AssignAndRemoveRequest;
import com.example.scheduler.entity.Patient;
import com.example.scheduler.entity.Personal;
import com.example.scheduler.exception.ForbiddenException;
import com.example.scheduler.exception.MessageKeys;
import com.example.scheduler.exception.ResourceNotFoundException;
import com.example.scheduler.mapper.PatientMapper;
import com.example.scheduler.repository.PatientRepository;
import com.example.scheduler.repository.PersonalRepository;
import com.example.scheduler.service.DoctorPatientService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DoctorPatientServiceImpl implements DoctorPatientService {
    private final PersonalRepository personalRepository;
    private final PatientRepository patientRepository;
    private final PatientMapper patientMapper;

    @Override
    @Transactional
    public void assignPatient(AssignAndRemoveRequest request, Long userId, boolean restrictToOwn) {
        verifyDoctorPermission(restrictToOwn, request.doctorId(), userId);
        var doctor = getDoctorPatientsOrThrowById(request.doctorId());
        var patient = getPatientOrThrowById(request.patientId());
        if (!doctor.getPatients().contains(patient)) {
            doctor.getPatients().add(patient);
            personalRepository.save(doctor);
        }
    }

    @Override
    @Transactional
    public void removePatient(AssignAndRemoveRequest request, Long userId, boolean restrictToOwn) {
        verifyDoctorPermission(restrictToOwn, request.doctorId(), userId);
        var doctor = getDoctorPatientsOrThrowById(request.doctorId());
        var patient = getPatientOrThrowById(request.patientId());
        if (doctor.getPatients().contains(patient)) {
            doctor.getPatients().remove(patient);
            personalRepository.save(doctor);
        }
    }

    @Override
    public List<PatientResponse> getPatientsOfDoctor(Long doctorId, Long userId, boolean restrictToOwn) {
        verifyDoctorPermission(restrictToOwn, doctorId, userId);
        var doctor = getDoctorPatientsOrThrowById(doctorId);
        return patientMapper.toResponseList(doctor.getPatients());
    }

    private Personal getDoctorPatientsOrThrowById(Long personalId) {
        return personalRepository
                .findDoctorPatientsById(personalId)
                .orElseThrow(() -> new ResourceNotFoundException(MessageKeys.PERSONAL_NOT_FOUND, personalId));
    }

    private Patient getPatientOrThrowById(Long patientId) {
        return patientRepository
                .findById(patientId)
                .orElseThrow(() -> new ResourceNotFoundException(MessageKeys.PATIENT_NOT_FOUND, patientId));
    }

    private static void verifyDoctorPermission(boolean restrictToOwn, Long doctorId, Long userId) {
        if (restrictToOwn && !doctorId.equals(userId)) throw new ForbiddenException(MessageKeys.COMMON_FORBIDDEN);
    }
}
