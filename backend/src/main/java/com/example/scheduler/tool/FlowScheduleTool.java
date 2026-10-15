package com.example.scheduler.tool;

import com.example.scheduler.dto.patient.PatientResponse;
import com.example.scheduler.dto.personal.PersonalResponse;
import com.example.scheduler.dto.specialty.SpecialtyResponse;
import com.example.scheduler.entity.Patient;
import com.example.scheduler.exception.ResourceNotFoundException;
import com.example.scheduler.mapper.*;
import com.example.scheduler.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;

@Component
@RequiredArgsConstructor
public class FlowScheduleTool {
    private final SpecialtyRepository specialtyRepository;
    private final SpecialtyMapper specialtyMapper;
    private final PersonalRepository personalRepository;
    private final PersonalMapper personalMapper;
    private final PatientRepository patientRepository;
    private final PatientMapper patientMapper;


    @Tool(description = "Step 1: Return the patient information", name = "getPatientUser")
    public PatientResponse getPatientUser(){
        var patientId = Objects.requireNonNull(SecurityContextHolder.getContext().getAuthentication()).getName();
        var patient = getPatientOrThrowById(Long.parseLong(patientId));
        return patientMapper.toResponse(patient);
    }

    @Tool(description = "Step 2: Return the specialties", name = "findAllSpecialties")
    public List<SpecialtyResponse> findAllSpecialties(){
        return specialtyMapper.toResponseList(specialtyRepository.findAll());
    }

    @Tool(description = "Step3: Return the doctors by specialtyId", name = "findAllDoctors")
    public List<PersonalResponse> findAllDoctors(Long specialtyId){
        if(specialtyId != null)
            getSpecialtyOrThrowById(specialtyId);
        return personalMapper.toResponseList(personalRepository.findAllDoctorsActive(specialtyId));
    }

    private void getSpecialtyOrThrowById(Long specialtyId) {
        specialtyRepository.findById(specialtyId)
                .orElseThrow(() -> new ResourceNotFoundException("Specialty not found with id: " + specialtyId));
    }

    private Patient getPatientOrThrowById(Long patientId) {
        return patientRepository.findById(patientId)
                .orElseThrow(() -> new  ResourceNotFoundException("Patient not found"));
    }
}
