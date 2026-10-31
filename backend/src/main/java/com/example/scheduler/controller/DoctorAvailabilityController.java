package com.example.scheduler.controller;

import com.example.scheduler.dto.doctorAvailability.DoctorAvailabilityRequest;
import com.example.scheduler.dto.doctorAvailability.DoctorAvailabilityResponse;
import com.example.scheduler.dto.doctorAvailability.DoctorAvailabilitySlotsResponse;
import com.example.scheduler.security.RoleExpressions;
import com.example.scheduler.security.SecurityUtils;
import com.example.scheduler.service.DoctorAvailabilityService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/doctorAvailability")
@RequiredArgsConstructor
@Tag(name = "Doctor Availabilities", description = "Doctor Availability Controller")
public class DoctorAvailabilityController {
    private final DoctorAvailabilityService doctorAvailabilityService;

    @PostMapping
    @PreAuthorize(RoleExpressions.DOCTOR)
    @Operation(summary = "POST /api/doctorAvailability — add a weekly availability block for the current doctor")
    public ResponseEntity<DoctorAvailabilityResponse> addDoctorAvailability(
            @Valid @RequestBody DoctorAvailabilityRequest request, Authentication auth) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(doctorAvailabilityService.addDoctorAvailability(SecurityUtils.extractUserId(auth), request));
    }

    @DeleteMapping("/{availabilityId}")
    @PreAuthorize(RoleExpressions.DOCTOR)
    @Operation(
            summary = "DELETE /api/doctorAvailability/{id} — remove a weekly availability block of the current doctor")
    public ResponseEntity<Void> deactivateDoctorAvailabilityById(
            @PathVariable Long availabilityId, Authentication auth) {
        doctorAvailabilityService.deactivateDoctorAvailabilityById(availabilityId, SecurityUtils.extractUserId(auth));
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me")
    @PreAuthorize(RoleExpressions.DOCTOR)
    @Operation(summary = "GET /api/doctorAvailability/me — list the weekly availability blocks of the current doctor")
    public ResponseEntity<List<DoctorAvailabilityResponse>> getAllDoctorAvailability(Authentication auth) {
        return ResponseEntity.ok(doctorAvailabilityService.getDoctorAvailabilities(SecurityUtils.extractUserId(auth)));
    }

    @GetMapping("/doctor/{doctorId}/availables")
    @PreAuthorize(RoleExpressions.STAFF)
    @Operation(
            summary =
                    "GET /api/doctorAvailability/doctor/{doctorId}/availables?date={date} — free slots (startTime/endTime) of a doctor on a date")
    public ResponseEntity<DoctorAvailabilitySlotsResponse> getDoctorAvailabilitySlots(
            @PathVariable Long doctorId, @RequestParam LocalDate date) {
        return ResponseEntity.ok(doctorAvailabilityService.getDoctorAvailableSlots(doctorId, date));
    }
}
