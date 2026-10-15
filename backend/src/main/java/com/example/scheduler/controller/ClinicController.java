package com.example.scheduler.controller;

import com.example.scheduler.dto.clinic.ClinicCreatedResponse;
import com.example.scheduler.dto.clinic.ClinicRequest;
import com.example.scheduler.dto.clinic.ClinicResponse;
import com.example.scheduler.service.ClinicService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/clinics")
@RequiredArgsConstructor
@Tag(name = "Clinics", description = "Clinics Controller")
public class ClinicController {

    private final ClinicService clinicService;

    @GetMapping
    @Operation(summary = "List all clinics (public)")
    public ResponseEntity<List<ClinicResponse>> findAllClinics() {
        return ResponseEntity.ok(clinicService.findAllClinics());
    }

    @PostMapping
    @Operation(summary = "Register a new clinic and create its administrator account (public)")
    public ResponseEntity<ClinicCreatedResponse> createClinic(@Valid @RequestBody ClinicRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(clinicService.createClinic(request));
    }
}
