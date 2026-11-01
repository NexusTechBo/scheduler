package com.example.scheduler.controller;

import com.example.scheduler.dto.appointment.AppointmentRequest;
import com.example.scheduler.dto.appointment.AppointmentResponse;
import com.example.scheduler.dto.appointment.AppointmentSummaryItem;
import com.example.scheduler.dto.appointment.CancelAppointmentRequest;
import com.example.scheduler.enums.AppointmentStatus;
import com.example.scheduler.enums.ERole;
import com.example.scheduler.security.RoleExpressions;
import com.example.scheduler.security.SecurityUtils;
import com.example.scheduler.service.AppointmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/appointments")
@RequiredArgsConstructor
@Tag(name = "Appointments", description = "Appointment Controller")
public class AppointmentController {
    private final AppointmentService appointmentService;

    @PostMapping
    @PreAuthorize(RoleExpressions.STAFF)
    @Operation(summary = "POST /api/appointments — book an appointment; every appointment starts PENDING")
    public ResponseEntity<AppointmentResponse> bookAppointment(@Valid @RequestBody AppointmentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(appointmentService.bookAppointment(request));
    }

    @GetMapping("/{appointmentId}")
    @PreAuthorize(RoleExpressions.STAFF)
    @Operation(summary = "GET /api/appointments/{id} — get an appointment details by id")
    public ResponseEntity<AppointmentResponse> findAppointmentById(
            @PathVariable Long appointmentId, Authentication auth) {
        return ResponseEntity.ok(appointmentService.findAppointmentById(
                appointmentId, SecurityUtils.extractUserId(auth), SecurityUtils.extractRole(auth)));
    }

    @GetMapping
    @PreAuthorize(RoleExpressions.STAFF)
    @Operation(
            summary =
                    "GET /api/appointments — list appointments, filtered by ?doctorId={id}&patientId={id}&status={status}")
    public ResponseEntity<Page<AppointmentResponse>> findAllAppointments(
            @RequestParam(required = false) Long doctorId,
            @RequestParam(required = false) Long patientId,
            @RequestParam(required = false) AppointmentStatus status,
            @PageableDefault(sort = "startTime", direction = Sort.Direction.ASC) Pageable pageable,
            Authentication auth) {
        return ResponseEntity.ok(
                appointmentService.findAllAppointments(scopedDoctorId(doctorId, auth), patientId, status, pageable));
    }

    @PatchMapping("/{appointmentId}/confirm")
    @PreAuthorize(RoleExpressions.STAFF)
    @Operation(summary = "PATCH /api/appointments/{id}/confirm — confirm a pending appointment")
    public ResponseEntity<AppointmentResponse> confirmAppointmentById(
            @PathVariable Long appointmentId, Authentication auth) {
        return ResponseEntity.ok(appointmentService.confirmAppointmentById(
                appointmentId, SecurityUtils.extractUserId(auth), SecurityUtils.extractRole(auth)));
    }

    @PatchMapping("/{appointmentId}/cancel")
    @PreAuthorize(RoleExpressions.STAFF)
    @Operation(
            summary =
                    "PATCH /api/appointments/{id}/cancel — cancel an appointment with an optional {reason} shown to the patient, releases the slot back to AVAILABLE")
    public ResponseEntity<AppointmentResponse> cancelAppointmentById(
            @PathVariable Long appointmentId,
            @Valid @RequestBody CancelAppointmentRequest request,
            Authentication auth) {
        return ResponseEntity.ok(appointmentService.cancelAppointmentById(
                appointmentId, request, SecurityUtils.extractUserId(auth), SecurityUtils.extractRole(auth)));
    }

    @GetMapping("/board")
    @PreAuthorize(RoleExpressions.STAFF)
    @Operation(
            summary =
                    "GET /api/appointments/board — appointments grouped by status, filtered by ?from={date}&to={date}&doctorId={id}&patientId={id}")
    public ResponseEntity<Map<AppointmentStatus, List<AppointmentSummaryItem>>> getBoardByRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) Long doctorId,
            @RequestParam(required = false) Long patientId,
            Authentication auth) {
        return ResponseEntity.ok(
                appointmentService.getBoardByRange(from, to, scopedDoctorId(doctorId, auth), patientId));
    }

    @GetMapping("/calendar")
    @PreAuthorize(RoleExpressions.STAFF)
    @Operation(
            summary =
                    "GET /api/appointments/calendar — appointments grouped by day, filtered by ?month={month}&year={year}&doctorId={id}&patientId={id}")
    public ResponseEntity<Map<String, List<AppointmentSummaryItem>>> getCalendar(
            @RequestParam @Min(1) @Max(12) int month,
            @RequestParam @Min(1900) @Max(9999) int year,
            @RequestParam(required = false) Long doctorId,
            @RequestParam(required = false) Long patientId,
            Authentication auth) {
        return ResponseEntity.ok(
                appointmentService.getCalendar(month, year, scopedDoctorId(doctorId, auth), patientId));
    }

    private static Long scopedDoctorId(Long doctorId, Authentication auth) {
        return ERole.DOCTOR.name().equals(SecurityUtils.extractRole(auth))
                ? SecurityUtils.extractUserId(auth)
                : doctorId;
    }
}
