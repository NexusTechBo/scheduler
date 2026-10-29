package com.example.scheduler.service.impl;

import com.example.scheduler.dto.guest.GuestSessionRequest;
import com.example.scheduler.dto.guest.GuestSessionResponse;
import com.example.scheduler.enums.ClinicStatus;
import com.example.scheduler.exception.ResourceNotFoundException;
import com.example.scheduler.repository.ClinicRepository;
import com.example.scheduler.security.JwtUtil;
import com.example.scheduler.service.GuestSessionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GuestSessionServiceImpl implements GuestSessionService {
    private final ClinicRepository clinicRepository;
    private final JwtUtil jwtUtil;
    private final Clock clock;

    @Override
    public GuestSessionResponse createGuestSession(GuestSessionRequest request) {
        var clinic = clinicRepository.findBySlug(request.slug())
                .filter(c -> c.getStatus() == ClinicStatus.ACTIVE)
                .orElseThrow(() -> new ResourceNotFoundException("guest.clinicUnavailable"));
        var expiresAt = Instant.now(clock).plus(JwtUtil.GUEST_TTL).truncatedTo(ChronoUnit.SECONDS);
        return new GuestSessionResponse(jwtUtil.generateGuest(clinic, expiresAt), expiresAt);
    }
}
