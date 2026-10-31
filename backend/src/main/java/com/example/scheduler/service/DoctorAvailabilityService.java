package com.example.scheduler.service;

import com.example.scheduler.dto.doctorAvailability.DoctorAvailabilityRequest;
import com.example.scheduler.dto.doctorAvailability.DoctorAvailabilityResponse;
import com.example.scheduler.dto.doctorAvailability.DoctorAvailabilitySlotsResponse;
import java.time.LocalDate;
import java.util.List;

public interface DoctorAvailabilityService {
    DoctorAvailabilityResponse addDoctorAvailability(Long doctorId, DoctorAvailabilityRequest request);

    List<DoctorAvailabilityResponse> getDoctorAvailabilities(Long doctorId);

    DoctorAvailabilitySlotsResponse getDoctorAvailableSlots(Long doctorId, LocalDate date);

    void deactivateDoctorAvailabilityById(Long availabilityId, Long doctorId);
}
