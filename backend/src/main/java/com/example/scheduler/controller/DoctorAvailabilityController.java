package com.example.scheduler.controller;

import com.example.scheduler.dto.DoctorAvailability.DoctorAvailabilityRequest;
import com.example.scheduler.dto.DoctorAvailability.DoctorAvailabilityResponse;
import com.example.scheduler.dto.DoctorAvailability.DoctorAvailabilitySlotsResponse;
import com.example.scheduler.service.DoctorAvailabilityService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/doctorAvailability")
@RequiredArgsConstructor
@Tag(name = "Doctor Availabilities", description = "Doctor Availability Controller")
public class DoctorAvailabilityController {
    private final DoctorAvailabilityService doctorAvailabilityService;

    @PostMapping
    @PreAuthorize("hasAnyRole('DOCTOR')")
    @Operation(summary = "POST /api/doctorAvailability — add a weekly availability block for the current doctor")
    public ResponseEntity<DoctorAvailabilityResponse> addDoctorAvailability(@Valid @RequestBody DoctorAvailabilityRequest request, Authentication auth){
        return ResponseEntity.status(HttpStatus.CREATED).body(doctorAvailabilityService.addDoctorAvailability(Long.parseLong(auth.getName()), request));
    }

    @DeleteMapping("/{availabilityId}")
    @PreAuthorize("hasRole('DOCTOR')")
    @Operation(summary = "DELETE /api/doctorAvailability/{id} — remove a weekly availability block of the current doctor")
    public ResponseEntity<Void> deactivateDoctorAvailabilityById(@PathVariable Long availabilityId, Authentication auth) {
        doctorAvailabilityService.deactivateDoctorAvailabilityById(availabilityId, Long.parseLong(auth.getName()));
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me")
    @PreAuthorize("hasRole('DOCTOR')")
    @Operation(summary = "GET /api/doctorAvailability/me — list the weekly availability blocks of the current doctor")
    public ResponseEntity<List<DoctorAvailabilityResponse>> getAllDoctorAvailability(Authentication auth){
        return ResponseEntity.ok(doctorAvailabilityService.getDoctorAvailabilities(Long.parseLong(auth.getName())));
    }

    @GetMapping("/doctor/{doctorId}/availables")
    @PreAuthorize("hasAnyRole('DOCTOR', 'ASSISTANT')")
    @Operation(summary = "GET /api/doctorAvailability/doctor/{doctorId}/availables?date={date} — free slots (startTime/endTime) of a doctor on a date")
    public ResponseEntity<DoctorAvailabilitySlotsResponse> getDoctorAvailabilitySlots(@PathVariable Long doctorId, @RequestParam LocalDate date){
        return ResponseEntity.ok(doctorAvailabilityService.getDoctorAvailableSlots(doctorId, date));
    }
}
