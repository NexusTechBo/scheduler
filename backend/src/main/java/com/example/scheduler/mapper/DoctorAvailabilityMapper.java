package com.example.scheduler.mapper;

import com.example.scheduler.dto.DoctorAvailability.DoctorAvailabilityResponse;
import com.example.scheduler.entity.DoctorAvailability;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface DoctorAvailabilityMapper {
    DoctorAvailabilityResponse toResponse(DoctorAvailability doctorAvailability);
    List<DoctorAvailabilityResponse> toResponseList(List<DoctorAvailability> doctorAvailability);
}
