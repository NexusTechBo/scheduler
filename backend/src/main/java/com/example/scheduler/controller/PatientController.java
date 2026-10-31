package com.example.scheduler.controller;

import com.example.scheduler.dto.patient.PatientRequest;
import com.example.scheduler.dto.patient.PatientResponse;
import com.example.scheduler.dto.personal.PersonalResponse;
import com.example.scheduler.security.RoleExpressions;
import com.example.scheduler.service.PatientService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/patients")
@RequiredArgsConstructor
@Tag(name = "Patients", description = "Patients Controller")
public class PatientController {
    private final PatientService patientService;

    @GetMapping
    @PreAuthorize(RoleExpressions.STAFF)
    @Operation(
            summary =
                    "GET /api/patients?search={text}&isActive={bool} — list patients, optionally filtered by name, CI, email or phone and by active state")
    public ResponseEntity<Page<PatientResponse>> findAllPatients(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Boolean isActive,
            @PageableDefault(sort = "id", direction = Sort.Direction.ASC) Pageable pageable) {
        return ResponseEntity.ok(patientService.findAllPatients(search, isActive, pageable));
    }

    @PostMapping
    @PreAuthorize(RoleExpressions.STAFF)
    @Operation(summary = "POST /api/patients — create a patient user")
    public ResponseEntity<PatientResponse> createPatient(@Valid @RequestBody PatientRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(patientService.createPatient(request));
    }

    @GetMapping("/{patientId}")
    @PreAuthorize(RoleExpressions.STAFF)
    @Operation(summary = "GET /api/patients/{patientId} — get a patient by id")
    public ResponseEntity<PatientResponse> findPatientById(@PathVariable Long patientId) {
        return ResponseEntity.ok(patientService.findPatientById(patientId));
    }

    @PutMapping("/{patientId}")
    @PreAuthorize(RoleExpressions.STAFF)
    @Operation(summary = "PUT /api/patients/{patientId} — update a patient by id")
    public ResponseEntity<PatientResponse> updatePatientById(
            @PathVariable Long patientId, @Valid @RequestBody PatientRequest request) {
        return ResponseEntity.ok(patientService.updatePatientById(patientId, request));
    }

    @DeleteMapping("/{patientId}")
    @PreAuthorize(RoleExpressions.STAFF)
    @Operation(summary = "DELETE /api/patients/{patientId} — deactivate a patient by id")
    public ResponseEntity<Void> deactivatePatientById(@PathVariable Long patientId) {
        patientService.deactivatePatientById(patientId);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{patientId}/activate")
    @PreAuthorize(RoleExpressions.STAFF)
    @Operation(summary = "PATCH /api/patients/{patientId}/activate — reactivate a patient by id")
    public ResponseEntity<PatientResponse> activatePatientById(@PathVariable Long patientId) {
        return ResponseEntity.ok(patientService.activatePatientById(patientId));
    }

    @GetMapping("/{patientId}/doctors")
    @PreAuthorize(RoleExpressions.STAFF)
    @Operation(summary = "GET /api/patients/{patientId}/doctors — list all doctors assigned to a patient")
    public ResponseEntity<List<PersonalResponse>> getDoctors(@PathVariable Long patientId) {
        return ResponseEntity.ok(patientService.getDoctorsOfPatient(patientId));
    }
}
