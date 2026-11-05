package com.example.scheduler.controller;

import com.example.scheduler.dto.scheduleexception.ScheduleExceptionRequest;
import com.example.scheduler.dto.scheduleexception.ScheduleExceptionResponse;
import com.example.scheduler.security.RoleExpressions;
import com.example.scheduler.security.SecurityUtils;
import com.example.scheduler.service.ScheduleExceptionService;
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
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/scheduleException")
@RequiredArgsConstructor
@Tag(name = "Schedule Exceptions", description = "Schedule Exception Controller")
public class ScheduleExceptionController {
    private final ScheduleExceptionService scheduleExceptionService;

    @PostMapping
    @PreAuthorize(RoleExpressions.DOCTOR)
    @Operation(
            summary =
                    "POST /api/scheduleException — block a day or a time range of the current doctor; if it overlaps active appointments, send cancelAppointments=true to cancel them (409 otherwise)")
    public ResponseEntity<ScheduleExceptionResponse> addDoctorScheduleException(
            @Valid @RequestBody ScheduleExceptionRequest request, Authentication auth) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(scheduleExceptionService.addDoctorScheduleException(SecurityUtils.extractUserId(auth), request));
    }

    @GetMapping
    @PreAuthorize(RoleExpressions.DOCTOR)
    @Operation(
            summary = "GET /api/scheduleException — list the current doctor's blocks from ?from={date} (default today)")
    public ResponseEntity<List<ScheduleExceptionResponse>> findDoctorScheduleExceptions(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from, Authentication auth) {
        return ResponseEntity.ok(
                scheduleExceptionService.findDoctorScheduleExceptions(SecurityUtils.extractUserId(auth), from));
    }

    @DeleteMapping("/{scheduleExceptionId}")
    @PreAuthorize(RoleExpressions.DOCTOR)
    @Operation(summary = "DELETE /api/scheduleException/{id} — remove a block of the current doctor")
    public ResponseEntity<Void> deleteDoctorScheduleExceptionById(
            @PathVariable Long scheduleExceptionId, Authentication auth) {
        scheduleExceptionService.deleteDoctorScheduleExceptionById(
                scheduleExceptionId, SecurityUtils.extractUserId(auth));
        return ResponseEntity.noContent().build();
    }
}
