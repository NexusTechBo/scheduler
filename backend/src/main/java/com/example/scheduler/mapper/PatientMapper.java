package com.example.scheduler.mapper;

import com.example.scheduler.dto.patient.PatientRegisterRequest;
import com.example.scheduler.dto.patient.PatientRequest;
import com.example.scheduler.dto.patient.PatientResponse;
import com.example.scheduler.entity.Patient;
import org.mapstruct.*;

import java.util.List;

@Mapper(componentModel = "spring")
public interface PatientMapper {
    @Mapping(source = "account.name", target = "name")
    @Mapping(source = "account.email", target = "email")
    @Mapping(source = "account.phoneNumber", target = "phoneNumber")
    PatientResponse toResponse(Patient patient);
    @Mapping(target = "account.name", source = "name")
    @Mapping(target = "account.email", source = "email")
    @Mapping(target = "account.phoneNumber", source = "phoneNumber")
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "role", ignore = true)
    @Mapping(target = "active", ignore = true)
    @Mapping(target = "doctors", ignore = true)
    void toEntityUpdated(PatientRequest request, @MappingTarget Patient patient);
    List<PatientResponse> toResponseList(List<Patient> patients);
}
