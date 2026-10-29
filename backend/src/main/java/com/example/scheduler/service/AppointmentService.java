package com.example.scheduler.service;

import com.example.scheduler.dto.appointment.AppointmentRequest;
import com.example.scheduler.dto.appointment.AppointmentResponse;
import com.example.scheduler.dto.appointment.AppointmentSummaryItem;
import com.example.scheduler.dto.appointment.CancelAppointmentRequest;
import com.example.scheduler.entity.Appointment;
import com.example.scheduler.entity.Personal;
import com.example.scheduler.enums.AppointmentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public interface AppointmentService {
    AppointmentResponse bookAppointment(AppointmentRequest request);
    LocalDateTime verifySlotBookable(Personal doctor, LocalDateTime startTime);
    AppointmentResponse findAppointmentById(Long appointmentId, Long userId, String role);
    Page<AppointmentResponse> findAllAppointments(Long doctorId, Long patientId, AppointmentStatus status, Pageable pageable);
    AppointmentResponse confirmAppointmentById(Long AppointmentId, Long userId, String role);
    AppointmentResponse cancelAppointmentById(Long AppointmentId, CancelAppointmentRequest request, Long userId, String role);
    void cancelAppointments(List<Appointment> appointments, String reason);
    Map<AppointmentStatus, List<AppointmentSummaryItem>> getBoardByRange(LocalDate from, LocalDate to, Long doctorId, Long patientId);
    Map<String, List<AppointmentSummaryItem>> getCalendar(int month, int year, Long doctorId, Long patientId);
}
