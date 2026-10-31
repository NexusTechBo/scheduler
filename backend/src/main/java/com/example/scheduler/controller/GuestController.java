package com.example.scheduler.controller;

import com.example.scheduler.dto.doctoravailability.DoctorAvailabilitySlotsResponse;
import com.example.scheduler.dto.guest.GuestAppointmentRequest;
import com.example.scheduler.dto.guest.GuestAppointmentResponse;
import com.example.scheduler.dto.guest.GuestClinicResponse;
import com.example.scheduler.dto.guest.GuestDoctorResponse;
import com.example.scheduler.dto.guest.GuestNewPatientRequest;
import com.example.scheduler.dto.guest.GuestPatientRequest;
import com.example.scheduler.dto.specialty.SpecialtyResponse;
import com.example.scheduler.security.RoleExpressions;
import com.example.scheduler.service.GuestService;
import com.example.scheduler.service.SpecialtyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/guest")
@RequiredArgsConstructor
@Tag(name = "Guest", description = "Public booking through a guest link")
public class GuestController {
    private final GuestService guestService;
    private final SpecialtyService specialtyService;

    @GetMapping("/clinic")
    @PreAuthorize(RoleExpressions.GUEST)
    @Operation(summary = "GET /api/guest/clinic — name and slug of the link's clinic")
    public ResponseEntity<GuestClinicResponse> findClinic() {
        return ResponseEntity.ok(guestService.findCurrentClinic());
    }

    @GetMapping("/specialties")
    @PreAuthorize(RoleExpressions.GUEST)
    @Operation(summary = "GET /api/guest/specialties — list the clinic's specialties")
    public ResponseEntity<List<SpecialtyResponse>> findAllSpecialties() {
        return ResponseEntity.ok(specialtyService.findAllSpecialties());
    }

    @GetMapping("/doctors")
    @PreAuthorize(RoleExpressions.GUEST)
    @Operation(summary = "GET /api/guest/doctors?specialtyId={id} — active doctors (id, name, specialty only)")
    public ResponseEntity<List<GuestDoctorResponse>> findAllDoctors(@RequestParam(required = false) Long specialtyId) {
        return ResponseEntity.ok(guestService.findAllActiveDoctors(specialtyId));
    }

    @GetMapping("/doctors/{doctorId}/availables")
    @PreAuthorize(RoleExpressions.GUEST)
    @Operation(
            summary =
                    "GET /api/guest/doctors/{doctorId}/availables?date={date} — free slots of an active doctor on a date")
    public ResponseEntity<DoctorAvailabilitySlotsResponse> getDoctorAvailableSlots(
            @PathVariable Long doctorId, @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(guestService.getDoctorAvailableSlots(doctorId, date));
    }

    @PostMapping("/appointments")
    @PreAuthorize(RoleExpressions.GUEST)
    @Operation(
            summary = "POST /api/guest/appointments — book a PENDING appointment for a registered patient (CI + phone)")
    public ResponseEntity<GuestAppointmentResponse> bookAppointment(
            @Valid @RequestBody GuestAppointmentRequest<GuestPatientRequest> request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(guestService.bookAppointment(request));
    }

    @PostMapping("/appointments/new-patient")
    @PreAuthorize(RoleExpressions.GUEST)
    @Operation(
            summary = "POST /api/guest/appointments/new-patient — register the patient and book a PENDING appointment")
    public ResponseEntity<GuestAppointmentResponse> bookAppointmentForNewPatient(
            @Valid @RequestBody GuestAppointmentRequest<GuestNewPatientRequest> request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(guestService.bookAppointmentForNewPatient(request));
    }
}
