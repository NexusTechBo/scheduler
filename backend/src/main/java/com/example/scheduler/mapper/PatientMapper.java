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
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "phoneNumber", source = "phoneNumber")
    void toEntityUpdated(PatientRequest request, @MappingTarget Patient patient, String phoneNumber);
    List<PatientResponse> toResponseList(List<Patient> patients);
}
