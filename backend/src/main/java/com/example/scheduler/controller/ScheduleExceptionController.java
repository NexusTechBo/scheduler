package com.example.scheduler.controller;

import com.example.scheduler.dto.ScheduleException.ScheduleExceptionRequest;
import com.example.scheduler.dto.ScheduleException.ScheduleExceptionResponse;
import com.example.scheduler.service.ScheduleExceptionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/scheduleException")
@RequiredArgsConstructor
@Tag(name = "Schedule Exceptions", description = "Schedule Exception Controller")
public class ScheduleExceptionController {
    private final ScheduleExceptionService scheduleExceptionService;

    @PostMapping
    @PreAuthorize("hasRole('DOCTOR')")
    @Operation(summary = "POST /api/scheduleException — block a day or a time range of the current doctor")
    public ResponseEntity<ScheduleExceptionResponse> addDoctorScheduleException(@RequestBody ScheduleExceptionRequest request, Authentication auth){
        return ResponseEntity.ok(scheduleExceptionService.addDoctorScheduleException(Long.parseLong(auth.getName()), request));
    }

    @GetMapping
    @PreAuthorize("hasRole('DOCTOR')")
    @Operation(summary = "GET /api/scheduleException — list the current doctor's blocks from ?from={date} (default today)")
    public ResponseEntity<List<ScheduleExceptionResponse>> findDoctorScheduleExceptions(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            Authentication auth) {
        return ResponseEntity.ok(scheduleExceptionService.findDoctorScheduleExceptions(Long.parseLong(auth.getName()), from));
    }

    @DeleteMapping("/{scheduleExceptionId}")
    @PreAuthorize("hasRole('DOCTOR')")
    @Operation(summary = "DELETE /api/scheduleException/{id} — remove a block of the current doctor")
    public ResponseEntity<Void> deleteDoctorScheduleExceptionById(@PathVariable Long scheduleExceptionId, Authentication auth) {
        scheduleExceptionService.deleteDoctorScheduleExceptionById(scheduleExceptionId, Long.parseLong(auth.getName()));
        return ResponseEntity.noContent().build();
    }
}
