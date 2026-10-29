package com.example.scheduler.controller;

import com.example.scheduler.dto.guest.GuestSessionRequest;
import com.example.scheduler.dto.guest.GuestSessionResponse;
import com.example.scheduler.service.GuestSessionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/public/guest")
@RequiredArgsConstructor
@Tag(name = "Guest Session", description = "Guest Session Controller")
public class GuestSessionController {
    private final GuestSessionService guestSessionService;

    @PostMapping("/session")
    @SecurityRequirements
    @Operation(summary = "POST /api/public/guest/session — short-lived GUEST token for an active clinic's public booking page (public)")
    public ResponseEntity<GuestSessionResponse> createGuestSession(@Valid @RequestBody GuestSessionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(guestSessionService.createGuestSession(request));
    }
}
