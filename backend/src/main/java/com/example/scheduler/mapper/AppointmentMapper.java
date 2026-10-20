package com.example.scheduler.mapper;

import com.example.scheduler.dto.appointment.AppointmentResponse;
import com.example.scheduler.entity.Appointment;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface AppointmentMapper {
    @Mapping(target = "doctorId", source = "doctor.id")
    @Mapping(target = "doctorName", source = "doctor.name")
    @Mapping(target = "doctorSpecialty", source = "doctor.specialty.name")
    @Mapping(target = "doctorEmail", source = "doctor.email")
    @Mapping(target = "patientId", source = "patient.id")
    @Mapping(target = "patientName", source = "patient.name")
    @Mapping(target = "patientEmail", source = "patient.email")
    AppointmentResponse toResponse(Appointment appointment);
}
