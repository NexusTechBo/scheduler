package com.example.scheduler.controller;

import com.example.scheduler.dto.patient.PatientResponse;
import com.example.scheduler.dto.personal.AssignAndRemoveRequest;
import com.example.scheduler.dto.personal.PersonalRegisterRequest;
import com.example.scheduler.dto.personal.PersonalRequest;
import com.example.scheduler.dto.personal.PersonalResponse;
import com.example.scheduler.security.SecurityUtils;
import com.example.scheduler.service.PersonalService;
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
@RequestMapping("/api/personal")
@RequiredArgsConstructor
@Tag(name = "Personal", description = "Personal Controller")
public class PersonalController {
    private final PersonalService personalService;

    @GetMapping("/doctors")
    @PreAuthorize("hasAnyRole('PATIENT', 'ASSISTANT')")
    @Operation(summary = "GET /api/personal/doctors — list all doctors, filter by ?specialtyId={specialtyId}?isActive={isActive}")
    public ResponseEntity<Page<PersonalResponse>> findAllDoctors(
            @RequestParam(required = false) Long specialtyId,
            @RequestParam(required = false) Boolean isActive,
            @PageableDefault(sort = "id", direction = Sort.Direction.ASC) Pageable pageable
    ) {
        return ResponseEntity.ok(personalService.findAllDoctors(specialtyId, isActive, pageable));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMINISTRATOR')")
    @Operation(summary = "GET /api/personal — list all personal, filter by ?specialtyId={specialtyId}&isActive={isActive}&role={role}")
    public ResponseEntity<Page<PersonalResponse>> findAllPersonal(
            @RequestParam(required = false) Long specialtyId,
            @RequestParam(required = false) Boolean isActive,
            @RequestParam(required = false) Long roleId,
            @PageableDefault(sort = "id", direction = Sort.Direction.ASC) Pageable pageable
    ) {
        return ResponseEntity.ok(personalService.findAllPersonal(specialtyId, isActive, roleId, pageable));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMINISTRATOR')")
    @Operation(summary = "POST /api/personal/{personalId} — create a personal user")
    public ResponseEntity<PersonalResponse> createPersonal(@Valid @RequestBody PersonalRegisterRequest request){
        return ResponseEntity.ok(personalService.createPersonal(request));
    }

    @GetMapping("/{personalId}")
    @PreAuthorize("hasAnyRole('ASSISTANT', 'ADMINISTRATOR')")
    @Operation(summary = "GET /api/personal/{personalId} — get a personal by id")
    public ResponseEntity<PersonalResponse> findPersonalById(@PathVariable Long personalId) {
        return ResponseEntity.ok(personalService.findPersonalById(personalId));
    }

    @PutMapping("/update/{personalId}")
    @PreAuthorize("hasAnyRole('ADMINISTRATOR')")
    @Operation(summary = "PUT /api/personal/{personalId} — update a personal by id")
    public ResponseEntity<PersonalResponse> updatePersonalById(@PathVariable Long personalId, @Valid @RequestBody PersonalRequest request) {
        return ResponseEntity.ok(personalService.updatePersonalById(personalId, request));
    }

    @PutMapping("/update")
    @PreAuthorize("hasAnyRole('DOCTOR', 'ASSISTANT')")
    @Operation(summary = "PUT /api/personal — update self personal information")
    public ResponseEntity<PersonalResponse> updatePersonalProfile(@Valid @RequestBody PersonalRequest request, Authentication auth) {
        // Self-update can't change the specialty; only an administrator can.
        var selfRequest = new PersonalRequest(request.name(), request.email(), null);
        return ResponseEntity.ok(personalService.updatePersonalById(Long.parseLong(auth.getName()), selfRequest));
    }

    @DeleteMapping("/{personalId}")
    @PreAuthorize("hasAnyRole('ADMINISTRATOR')")
    @Operation(summary = "DELETE /api/personal/{personalId} — deactivate a personal by id")
    public ResponseEntity<Void> deactivatePersonalById(@PathVariable Long personalId) {
        personalService.deactivatePersonalById(personalId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/patients/assign")
    @PreAuthorize("hasAnyRole('DOCTOR', 'ASSISTANT')")
    @Operation(summary = "POST /api/personal/patients/assign — assign a patient to a doctor")
    public ResponseEntity<Void> assignPatient(@Valid @RequestBody AssignAndRemoveRequest request, Authentication auth) {
        personalService.assignPatient(request, Long.parseLong(auth.getName()), SecurityUtils.extractRole(auth));
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/patients/remove")
    @PreAuthorize("hasAnyRole('DOCTOR', 'ASSISTANT')")
    @Operation(summary = "DELETE /api/personal/patients/remove — remove a patient from a doctor")
    public ResponseEntity<Void> removePatient(@Valid @RequestBody AssignAndRemoveRequest request, Authentication auth) {
        personalService.removePatient(request, Long.parseLong(auth.getName()), SecurityUtils.extractRole(auth));
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{doctorId}/patients")
    @PreAuthorize("hasAnyRole('DOCTOR', 'ASSISTANT')")
    @Operation(summary = "GET /api/personal/{doctorId}/patients — list all patients assigned to a doctor")
    public ResponseEntity<List<PatientResponse>> getPatients(@PathVariable Long doctorId) {
        return ResponseEntity.ok(personalService.getPatientsOfDoctor(doctorId));
    }
}
