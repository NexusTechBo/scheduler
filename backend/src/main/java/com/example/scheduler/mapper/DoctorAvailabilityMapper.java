package com.example.scheduler.mapper;

import com.example.scheduler.dto.doctorAvailability.DoctorAvailabilityResponse;
import com.example.scheduler.entity.DoctorAvailability;
import java.util.List;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface DoctorAvailabilityMapper {
    DoctorAvailabilityResponse toResponse(DoctorAvailability doctorAvailability);

    List<DoctorAvailabilityResponse> toResponseList(List<DoctorAvailability> doctorAvailability);
}
