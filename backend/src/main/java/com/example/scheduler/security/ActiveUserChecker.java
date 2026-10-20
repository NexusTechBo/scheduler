package com.example.scheduler.security;

import com.example.scheduler.enums.ERole;
import com.example.scheduler.repository.PersonalRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Arrays;

@Component
@RequiredArgsConstructor
public class ActiveUserChecker {
    private final PersonalRepository personalRepository;

    /**
     * Checks that the token's user is still active and still has the role the token claims,
     * so a token issued before an admin changed the role stops working.
     *
     * @param userId the JWT subject
     * @param role   the JWT role claim
     * @return {@code true} if the user is active with that role; {@code false} for unknown roles
     */
    public boolean isActive(Long userId, String role) {
        return Arrays.stream(ERole.values())
                .filter(r -> r.name().equals(role))
                .findFirst()
                .map(r -> personalRepository.existsByIdAndActiveTrueAndRole(userId, r))
                .orElse(false);
    }
}
