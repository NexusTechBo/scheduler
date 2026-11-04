package com.example.scheduler.security;

import com.example.scheduler.enums.ERole;
import com.example.scheduler.repository.PersonalRepository;
import java.util.Arrays;
import java.util.Collection;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ActiveUserChecker {
    private final PersonalRepository personalRepository;

    public boolean isActive(Long userId, String role, Collection<String> roles) {
        if (ERole.GUEST.name().equals(role)) return true;
        var primary = Arrays.stream(ERole.values())
                .filter(r -> r.name().equals(role))
                .findFirst()
                .orElse(null);
        if (primary == null) return false;
        var alsoDoctor = primary == ERole.ADMINISTRATOR && roles.contains(ERole.DOCTOR.name());
        var expected = alsoDoctor ? Set.of(primary.name(), ERole.DOCTOR.name()) : Set.of(primary.name());
        if (!expected.equals(Set.copyOf(roles))) return false;
        return personalRepository.existsByIdAndActiveTrueAndRoleAndAlsoDoctor(userId, primary, alsoDoctor);
    }
}
