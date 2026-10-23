package com.example.scheduler.service.impl;

import com.example.scheduler.config.tenant.TenantContext;
import com.example.scheduler.dto.login.LoginRequest;
import com.example.scheduler.dto.login.LoginResponse;
import com.example.scheduler.dto.personal.PersonalResponse;
import com.example.scheduler.enums.ClinicStatus;
import com.example.scheduler.exception.BadRequestException;
import com.example.scheduler.exception.ForbiddenException;
import com.example.scheduler.exception.ResourceNotFoundException;
import com.example.scheduler.exception.UnauthorizedException;
import com.example.scheduler.mapper.PersonalMapper;
import com.example.scheduler.repository.ClinicRepository;
import com.example.scheduler.repository.PersonalRepository;
import com.example.scheduler.security.JwtUtil;
import com.example.scheduler.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final ClinicRepository clinicRepository;
    private final PersonalRepository personalRepository;
    private final PersonalMapper personalMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    @Override
    public LoginResponse login(LoginRequest request) {
        var username = request.username().toLowerCase(Locale.ROOT);
        var at = username.lastIndexOf('@');
        if (at < 0)
            throw new BadRequestException("Invalid Credentials");
        var clinic = clinicRepository.findBySubdomain(username.substring(at))
                .orElseThrow(() -> new UnauthorizedException("Invalid Credentials"));
        try {
            TenantContext.setCurrentTenant("clinic_" + clinic.getId());
            var personal = personalRepository.findByUsername(username)
                    .orElseThrow(() -> new UnauthorizedException("Invalid Credentials"));
            if (!personal.isActive())
                throw new UnauthorizedException("Your user is not active");
            if (!passwordEncoder.matches(request.password(), personal.getPassword()))
                throw new UnauthorizedException("Invalid Credentials");
            if (clinic.getStatus() != ClinicStatus.ACTIVE)
                throw new ForbiddenException("The clinic has not been activated yet. Check your email to activate it");
            return new LoginResponse(jwtUtil.generate(personal.getId(), personal.getRole().name(), clinic, personal.getName()));
        } finally {
            TenantContext.clear();
        }
    }

    @Override
    @Transactional(readOnly = true)
    public PersonalResponse findProfile(Long userId) {
        return personalRepository.findById(userId)
                .map(personalMapper::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Personal not found with id: " + userId));
    }
}
