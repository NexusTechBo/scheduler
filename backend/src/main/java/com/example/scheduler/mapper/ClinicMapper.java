package com.example.scheduler.mapper;

import com.example.scheduler.dto.clinic.ClinicResponse;
import com.example.scheduler.entity.Clinic;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface ClinicMapper {
    List<ClinicResponse> toResponseList(List<Clinic> clinics);
}
