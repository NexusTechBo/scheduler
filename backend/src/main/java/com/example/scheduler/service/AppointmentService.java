package com.example.scheduler.service;

import com.example.scheduler.dto.appointment.AppointmentRequest;
import com.example.scheduler.dto.appointment.AppointmentResponse;
import com.example.scheduler.dto.appointment.AppointmentSummaryItem;
import com.example.scheduler.dto.appointment.CancelAppointmentRequest;
import com.example.scheduler.enums.AppointmentStatus;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface AppointmentService {
    AppointmentResponse bookAppointment(AppointmentRequest request, Long userId, boolean restrictToOwn);

    AppointmentResponse bookGuestAppointment(AppointmentRequest request);

    AppointmentResponse findAppointmentById(Long appointmentId, Long userId, boolean restrictToOwn);

    Page<AppointmentResponse> findAllAppointments(
            Long doctorId, Long patientId, AppointmentStatus status, Pageable pageable);

    AppointmentResponse confirmAppointmentById(Long appointmentId, Long userId, boolean restrictToOwn);

    AppointmentResponse cancelAppointmentById(
            Long appointmentId, CancelAppointmentRequest request, Long userId, boolean restrictToOwn);

    int cancelDoctorAppointmentsInRange(Long doctorId, LocalDateTime from, LocalDateTime to, String reason);

    int cancelUpcomingAppointmentsOfDeactivatedDoctor(Long doctorId);

    int cancelUpcomingAppointmentsOfDeactivatedPatient(Long patientId);

    Map<AppointmentStatus, List<AppointmentSummaryItem>> getBoardByRange(
            LocalDate from, LocalDate to, Long doctorId, Long patientId);

    Map<String, List<AppointmentSummaryItem>> getCalendar(int month, int year, Long doctorId, Long patientId);
}
