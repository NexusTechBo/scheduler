package com.example.scheduler.controller;

import com.example.scheduler.dto.clinic.ActivateClinicRequest;
import com.example.scheduler.dto.clinic.ClinicActivatedResponse;
import com.example.scheduler.dto.clinic.ClinicCreatedResponse;
import com.example.scheduler.dto.clinic.ClinicRequest;
import com.example.scheduler.dto.clinic.ResendActivationRequest;
import com.example.scheduler.service.ClinicActivationService;
import com.example.scheduler.service.ClinicService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/clinics")
@RequiredArgsConstructor
@Tag(name = "Clinics", description = "Clinics Controller")
public class ClinicController {
    private final ClinicService clinicService;
    private final ClinicActivationService clinicActivationService;

    @PostMapping
    @Operation(
            summary = "Register a new clinic and create its administrator account; sends the activation email (public)")
    public ResponseEntity<ClinicCreatedResponse> createClinic(@Valid @RequestBody ClinicRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(clinicService.createClinic(request));
    }

    @PostMapping("/activate")
    @Operation(summary = "Activate a clinic with the token received by email (public)")
    public ResponseEntity<ClinicActivatedResponse> activateClinic(@Valid @RequestBody ActivateClinicRequest request) {
        return ResponseEntity.ok(clinicActivationService.activateClinic(request));
    }

    @PostMapping("/activation/resend")
    @Operation(summary = "Resend the activation email of a pending clinic; always 202 (public)")
    public ResponseEntity<Void> resendActivation(@Valid @RequestBody ResendActivationRequest request) {
        clinicActivationService.resendActivation(request);
        return ResponseEntity.accepted().build();
    }
}
