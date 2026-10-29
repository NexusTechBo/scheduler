package com.example.scheduler.controller;

import com.example.scheduler.dto.DoctorAvailability.DoctorAvailabilitySlotsResponse;
import com.example.scheduler.dto.guest.GuestAppointmentRequest;
import com.example.scheduler.dto.guest.GuestAppointmentResponse;
import com.example.scheduler.dto.guest.GuestClinicResponse;
import com.example.scheduler.dto.guest.GuestDoctorResponse;
import com.example.scheduler.dto.specialty.SpecialtyResponse;
import com.example.scheduler.service.GuestService;
import com.example.scheduler.service.SpecialtyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/guest")
@RequiredArgsConstructor
@Tag(name = "Guest", description = "Public booking through a guest link")
public class GuestController {
    private final GuestService guestService;
    private final SpecialtyService specialtyService;

    @GetMapping("/clinic")
    @PreAuthorize("hasRole('GUEST')")
    @Operation(summary = "GET /api/guest/clinic — name and slug of the link's clinic")
    public ResponseEntity<GuestClinicResponse> findClinic() {
        return ResponseEntity.ok(guestService.findCurrentClinic());
    }

    @GetMapping("/specialties")
    @PreAuthorize("hasRole('GUEST')")
    @Operation(summary = "GET /api/guest/specialties — list the clinic's specialties")
    public ResponseEntity<List<SpecialtyResponse>> findAllSpecialties() {
        return ResponseEntity.ok(specialtyService.findAllSpecialties());
    }

    @GetMapping("/doctors")
    @PreAuthorize("hasRole('GUEST')")
    @Operation(summary = "GET /api/guest/doctors?specialtyId={id} — active doctors (id, name, specialty only)")
    public ResponseEntity<List<GuestDoctorResponse>> findAllDoctors(@RequestParam(required = false) Long specialtyId) {
        return ResponseEntity.ok(guestService.findAllActiveDoctors(specialtyId));
    }

    @GetMapping("/doctors/{doctorId}/availables")
    @PreAuthorize("hasRole('GUEST')")
    @Operation(summary = "GET /api/guest/doctors/{doctorId}/availables?date={date} — free slots of an active doctor on a date")
    public ResponseEntity<DoctorAvailabilitySlotsResponse> getDoctorAvailableSlots(
            @PathVariable Long doctorId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(guestService.getDoctorAvailableSlots(doctorId, date));
    }

    @PostMapping("/appointments")
    @PreAuthorize("hasRole('GUEST')")
    @Operation(summary = "POST /api/guest/appointments — book a PENDING appointment, reusing the patient by CI or creating it")
    public ResponseEntity<GuestAppointmentResponse> bookAppointment(@Valid @RequestBody GuestAppointmentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(guestService.bookAppointment(request));
    }
}
