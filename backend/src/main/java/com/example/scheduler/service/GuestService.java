package com.example.scheduler.service;

import com.example.scheduler.dto.doctoravailability.DoctorAvailabilitySlotsResponse;
import com.example.scheduler.dto.guest.GuestAppointmentRequest;
import com.example.scheduler.dto.guest.GuestAppointmentResponse;
import com.example.scheduler.dto.guest.GuestClinicResponse;
import com.example.scheduler.dto.guest.GuestDoctorResponse;
import com.example.scheduler.dto.guest.GuestNewPatientRequest;
import com.example.scheduler.dto.guest.GuestPatientRequest;
import java.time.LocalDate;
import java.util.List;

public interface GuestService {
    GuestClinicResponse findCurrentClinic();

    List<GuestDoctorResponse> findAllActiveDoctors(Long specialtyId);

    DoctorAvailabilitySlotsResponse getDoctorAvailableSlots(Long doctorId, LocalDate date);

    GuestAppointmentResponse bookAppointment(GuestAppointmentRequest<GuestPatientRequest> request);

    GuestAppointmentResponse bookAppointmentForNewPatient(GuestAppointmentRequest<GuestNewPatientRequest> request);
}
