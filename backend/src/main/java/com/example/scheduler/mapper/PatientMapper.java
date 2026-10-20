package com.example.scheduler.mapper;

import com.example.scheduler.dto.patient.PatientRequest;
import com.example.scheduler.dto.patient.PatientResponse;
import com.example.scheduler.entity.Patient;
import org.mapstruct.*;

import java.util.List;

@Mapper(componentModel = "spring")
public interface PatientMapper {
    PatientResponse toResponse(Patient patient);
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "active", ignore = true)
    @Mapping(target = "doctors", ignore = true)
    Patient toEntity(PatientRequest request);
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "active", ignore = true)
    @Mapping(target = "doctors", ignore = true)
    void toEntityUpdated(PatientRequest request, @MappingTarget Patient patient);
    List<PatientResponse> toResponseList(List<Patient> patients);
}
