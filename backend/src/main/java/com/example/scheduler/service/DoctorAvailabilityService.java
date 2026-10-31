package com.example.scheduler.service;

import com.example.scheduler.dto.doctoravailability.DoctorAvailabilityRequest;
import com.example.scheduler.dto.doctoravailability.DoctorAvailabilityResponse;
import com.example.scheduler.dto.doctoravailability.DoctorAvailabilitySlotsResponse;
import java.time.LocalDate;
import java.util.List;

public interface DoctorAvailabilityService {
    DoctorAvailabilityResponse addDoctorAvailability(Long doctorId, DoctorAvailabilityRequest request);

    List<DoctorAvailabilityResponse> getDoctorAvailabilities(Long doctorId);

    DoctorAvailabilitySlotsResponse getDoctorAvailableSlots(Long doctorId, LocalDate date);

    void deactivateDoctorAvailabilityById(Long availabilityId, Long doctorId);
}
