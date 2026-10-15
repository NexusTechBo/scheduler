package com.example.scheduler.security;

import com.example.scheduler.enums.ERole;
import com.example.scheduler.repository.PatientRepository;
import com.example.scheduler.repository.PersonalRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Checks that the user behind a token is still active, so a deactivated user loses access at once.
 * Patient and personal ids live in different tables; the current tenant must already be set.
 */
@Component
@RequiredArgsConstructor
public class ActiveUserChecker {
    private final PatientRepository patientRepository;
    private final PersonalRepository personalRepository;

    public boolean isActive(Long userId, String role) {
        return ERole.PATIENT.name().equals(role)
                ? patientRepository.existsByIdAndActiveTrue(userId)
                : personalRepository.existsByIdAndActiveTrue(userId);
    }
}
