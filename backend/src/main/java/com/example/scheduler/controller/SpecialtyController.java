package com.example.scheduler.controller;

import com.example.scheduler.dto.specialty.SpecialtyRequest;
import com.example.scheduler.dto.specialty.SpecialtyResponse;
import com.example.scheduler.service.SpecialtyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/specialties")
@RequiredArgsConstructor
@Tag(name = "Specialties", description = "Specialties Controller")
public class SpecialtyController {
    private final SpecialtyService specialtyService;

    @GetMapping
    @Operation(summary = "GET /api/specialties — list all available specialties")
    public ResponseEntity<List<SpecialtyResponse>> findAllSpecialties() {
        return ResponseEntity.ok(specialtyService.findAllSpecialties());
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    @Operation(summary = "POST /api/specialties — create a specialty (administrator only)")
    public ResponseEntity<SpecialtyResponse> createSpecialty(@Valid @RequestBody SpecialtyRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(specialtyService.createSpecialty(request));
    }
}
