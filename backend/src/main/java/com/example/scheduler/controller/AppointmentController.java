package com.example.scheduler.controller;

import com.example.scheduler.dto.appointment.AppointmentRequest;
import com.example.scheduler.dto.appointment.AppointmentResponse;
import com.example.scheduler.dto.appointment.AppointmentSummaryItem;
import com.example.scheduler.dto.appointment.CancelAppointmentRequest;
import com.example.scheduler.enums.AppointmentStatus;
import com.example.scheduler.enums.ERole;
import com.example.scheduler.security.SecurityUtils;
import com.example.scheduler.service.AppointmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
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

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/appointments")
@RequiredArgsConstructor
@Tag(name = "Appointments", description = "Appointment Controller")
public class AppointmentController {
    private final AppointmentService appointmentService;

    @PostMapping
    @Operation(summary = "POST /api/appointments — book an appointment; every appointment starts PENDING; a PATIENT always books for itself")
    public ResponseEntity<AppointmentResponse> bookAppointment(@Valid @RequestBody AppointmentRequest request, Authentication auth) {
        return ResponseEntity.status(HttpStatus.CREATED).body(appointmentService.bookAppointment(request, Long.parseLong(auth.getName()), SecurityUtils.extractRole(auth)));
    }

    @GetMapping("/{appointmentId}")
    @Operation(summary = "GET /api/appointments/{id} — get an appointment details by id")
    public ResponseEntity<AppointmentResponse> findAppointmentById(@PathVariable Long appointmentId, Authentication auth) {
        return ResponseEntity.ok(appointmentService.findAppointmentById(appointmentId, Long.parseLong(auth.getName()), SecurityUtils.extractRole(auth)));
    }

    @GetMapping
    @Operation(summary = "GET /api/appointments — list appointments, filtered by ?doctorId={id}&patientId={id}&status={status}")
    public ResponseEntity<Page<AppointmentResponse>> findAllAppointments(
            @RequestParam(required = false) Long doctorId,
            @RequestParam(required = false) Long patientId,
            @RequestParam(required = false) AppointmentStatus status,
            @PageableDefault(sort = "startTime", direction = Sort.Direction.ASC) Pageable pageable,
            Authentication auth
    ) {
        var role = SecurityUtils.extractRole(auth);
        if(role.equals(ERole.DOCTOR.name())){
            return ResponseEntity.ok(appointmentService.findAllAppointments(
                    Long.parseLong(auth.getName()),
                    patientId,
                    status,
                    pageable)
            );
        }
        if(role.equals(ERole.PATIENT.name())){
            return ResponseEntity.ok(appointmentService.findAllAppointments(
                    doctorId,
                    Long.parseLong(auth.getName()),
                    status,
                    pageable)
            );
        }
        return ResponseEntity.ok(appointmentService.findAllAppointments(
                doctorId,
                patientId,
                status,
                pageable)
        );
    }

    @PatchMapping("/{appointmentId}/confirm")
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'DOCTOR', 'ASSISTANT')")
    @Operation(summary = "PATCH /api/appointments/{id}/confirm — confirm a pending appointment")
    public ResponseEntity<AppointmentResponse> confirmAppointmentById(@PathVariable Long appointmentId, Authentication auth) {
        return ResponseEntity.ok(appointmentService.confirmAppointmentById(appointmentId, Long.parseLong(auth.getName()), SecurityUtils.extractRole(auth)));
    }

    @PatchMapping("/{appointmentId}/cancel")
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'DOCTOR', 'ASSISTANT', 'PATIENT')")
    @Operation(summary = "PATCH /api/appointments/{id}/cancel — cancel an appointment with an optional {reason} shown to the patient, releases the slot back to AVAILABLE")
    public ResponseEntity<AppointmentResponse> cancelAppointmentById(
            @PathVariable Long appointmentId,
            @Valid @RequestBody CancelAppointmentRequest request,
            Authentication auth) {
        return ResponseEntity.ok(appointmentService.cancelAppointmentById(appointmentId, request, Long.parseLong(auth.getName()), SecurityUtils.extractRole(auth)));
    }

    @GetMapping("/board")
    @Operation(summary = "GET /api/appointments/board — appointments grouped by status, filtered by ?from={date}&to={date}&doctorId={id}&clientId={id}")
    public ResponseEntity<Map<AppointmentStatus, List<AppointmentSummaryItem>>> getBoardByRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) Long doctorId,
            @RequestParam(required = false) Long patientId,
            Authentication auth) {
        var role = SecurityUtils.extractRole(auth);
        if(role.equals(ERole.DOCTOR.name())){
            return ResponseEntity.ok(appointmentService.getBoardByRange(
                    from,
                    to,
                    Long.parseLong(auth.getName()),
                    patientId)
            );
        }
        if(role.equals(ERole.PATIENT.name())){
            return ResponseEntity.ok(appointmentService.getBoardByRange(
                    from,
                    to,
                    doctorId,
                    Long.parseLong(auth.getName()))
            );
        }
        return ResponseEntity.ok(appointmentService.getBoardByRange(
                from,
                to,
                doctorId,
                patientId)
        );
    }

    @GetMapping("/calendar")
    @Operation(summary = "GET /api/appointments/calendar — appointments grouped by day, filtered by ?month={month}&year={year}&doctorId={id}&clientId={id}")
    public ResponseEntity<Map<String, List<AppointmentSummaryItem>>> getCalendar(
            @RequestParam @Min(1) @Max(12) int month,
            @RequestParam @Min(1900) @Max(9999) int year,
            @RequestParam(required = false) Long doctorId,
            @RequestParam(required = false) Long patientId,
            Authentication auth) {
        var role = SecurityUtils.extractRole(auth);
        if(role.equals(ERole.DOCTOR.name())){
            return ResponseEntity.ok(appointmentService.getCalendar(
                    month,
                    year,
                    Long.parseLong(auth.getName()),
                    patientId)
            );
        }
        if(role.equals(ERole.PATIENT.name())){
            return ResponseEntity.ok(appointmentService.getCalendar(
                    month,
                    year,
                    doctorId,
                    Long.parseLong(auth.getName()))
            );
        }
        return ResponseEntity.ok(appointmentService.getCalendar(
                month,
                year,
                doctorId,
                patientId)
        );
    }
}
