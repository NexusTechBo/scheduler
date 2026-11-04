package com.example.scheduler.service;

import com.example.scheduler.dto.patient.PatientResponse;
import com.example.scheduler.dto.personal.AssignAndRemoveRequest;
import java.util.List;

public interface DoctorPatientService {
    void assignPatient(AssignAndRemoveRequest request, Long userId, boolean restrictToOwn);

    void removePatient(AssignAndRemoveRequest request, Long userId, boolean restrictToOwn);

    List<PatientResponse> getPatientsOfDoctor(Long doctorId, Long userId, boolean restrictToOwn);
}
