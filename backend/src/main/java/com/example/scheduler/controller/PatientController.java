package com.example.scheduler.controller;

import com.example.scheduler.dto.patient.PatientRegisterRequest;
import com.example.scheduler.dto.patient.PatientRequest;
import com.example.scheduler.dto.patient.PatientResponse;
import com.example.scheduler.dto.personal.PersonalResponse;
import com.example.scheduler.service.PatientService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/patients")
@RequiredArgsConstructor
@Tag(name = "Patients", description = "Patients Controller")
public class PatientController {
    private final PatientService patientService;

    @GetMapping
    @PreAuthorize("hasAnyRole('DOCTOR', 'ASSISTANT')")
    @Operation(summary = "GET /api/patients — list all patients")
    public ResponseEntity<Page<PatientResponse>> findAllPatients(@PageableDefault(sort = "id", direction = Sort.Direction.ASC) Pageable pageable) {
        return ResponseEntity.ok(patientService.findAllPatients(pageable));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('DOCTOR', 'ASSISTANT')")
    @Operation(summary = "POST /api/patients — create a patient user")
    public ResponseEntity<PatientResponse> createPatient(@Valid @RequestBody PatientRegisterRequest request) {
        return ResponseEntity.ok(patientService.createPatient(request));
    }

    @GetMapping("/{patientId}")
    @PreAuthorize("hasAnyRole('DOCTOR', 'ASSISTANT')")
    @Operation(summary = "GET /api/patients/{patientId} — get a patient by id")
    public ResponseEntity<PatientResponse> findPatientById(@PathVariable Long patientId) {
        return ResponseEntity.ok(patientService.findPatientById(patientId));
    }

    @PutMapping("/update/{patientId}")
    @PreAuthorize("hasAnyRole('DOCTOR', 'ASSISTANT')")
    @Operation(summary = "PUT /api/patients/{patientId} — update a patient by id")
    public ResponseEntity<PatientResponse> updatePatientById(@PathVariable Long patientId, @Valid @RequestBody PatientRequest request) {
        return ResponseEntity.ok(patientService.updatePatientById(patientId, request));
    }

    @PutMapping("/update")
    @PreAuthorize("hasAnyRole('PATIENT')")
    @Operation(summary = "PUT /api/patients — update self patient information")
    public ResponseEntity<PatientResponse> updatePatientProfile(@Valid @RequestBody PatientRequest request, Authentication auth) {
        return ResponseEntity.ok(patientService.updatePatientById(Long.parseLong(auth.getName()), request));
    }

    @DeleteMapping("/{patientId}")
    @PreAuthorize("hasAnyRole('DOCTOR', 'ASSISTANT')")
    @Operation(summary = "DELETE /api/patients/{patientId} — deactivate a patient by id")
    public ResponseEntity<Void> deactivatePatientById(@PathVariable Long patientId) {
        patientService.deactivatePatientById(patientId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{patientId}/doctors")
    @PreAuthorize("hasAnyRole('PATIENT', 'ASSISTANT')")
    @Operation(summary = "GET /api/patients/{patientId}/doctors — list all doctors assigned to a patient")
    public ResponseEntity<List<PersonalResponse>> getDoctors(@PathVariable Long patientId) {
        return ResponseEntity.ok(patientService.getDoctorsOfPatient(patientId));
    }
}
