package com.example.scheduler.security;

import com.example.scheduler.enums.ERole;
import com.example.scheduler.repository.PersonalRepository;
import java.util.Arrays;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ActiveUserChecker {
    private final PersonalRepository personalRepository;

    public boolean isActive(Long userId, String role) {
        if (ERole.GUEST.name().equals(role)) return true;
        return Arrays.stream(ERole.values())
                .filter(r -> r.name().equals(role))
                .findFirst()
                .map(r -> personalRepository.existsByIdAndActiveTrueAndRole(userId, r))
                .orElse(false);
    }
}
