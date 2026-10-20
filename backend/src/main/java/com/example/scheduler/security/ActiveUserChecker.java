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

    public boolean isActive(Long userId) {
        return personalRepository.existsByIdAndActiveTrue(userId);
    }
}
