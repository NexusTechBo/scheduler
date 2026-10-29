package com.example.scheduler.service;

import com.example.scheduler.dto.guest.GuestSessionRequest;
import com.example.scheduler.dto.guest.GuestSessionResponse;

public interface GuestSessionService {
    GuestSessionResponse createGuestSession(GuestSessionRequest request);
}
