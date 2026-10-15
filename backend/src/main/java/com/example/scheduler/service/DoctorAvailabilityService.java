package com.example.scheduler.service;

import com.example.scheduler.dto.DoctorAvailability.DoctorAvailabilitySlotsResponse;
import com.example.scheduler.dto.DoctorAvailability.DoctorAvailabilityRequest;
import com.example.scheduler.dto.DoctorAvailability.DoctorAvailabilityResponse;

import java.time.LocalDate;
import java.util.List;

public interface DoctorAvailabilityService {
    DoctorAvailabilityResponse addDoctorAvailability(Long doctorId, DoctorAvailabilityRequest request);
    List<DoctorAvailabilityResponse> getDoctorAvailabilities(Long doctorId);
    DoctorAvailabilitySlotsResponse getDoctorAvailableSlots(Long doctorId, LocalDate date);
    void deactivateDoctorAvailabilityById(Long availabilityId, Long doctorId);
}
