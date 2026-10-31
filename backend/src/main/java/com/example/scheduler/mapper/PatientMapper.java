package com.example.scheduler.mapper;

import com.example.scheduler.dto.patient.PatientResponse;
import com.example.scheduler.entity.Patient;
import java.util.List;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface PatientMapper {
    PatientResponse toResponse(Patient patient);

    List<PatientResponse> toResponseList(List<Patient> patients);
}
