package com.example.scheduler.service.impl;

import com.example.scheduler.config.tenant.TenantContext;
import com.example.scheduler.dto.login.LoginRequest;
import com.example.scheduler.dto.login.LoginResponse;
import com.example.scheduler.dto.personal.PersonalResponse;
import com.example.scheduler.enums.ClinicStatus;
import com.example.scheduler.exception.ApiException;
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
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {
    private static final Pattern CONTROL_CHARS = Pattern.compile("\\p{Cntrl}");
    private static final int MAX_LOGGED_USERNAME = 100;

    private final ClinicRepository clinicRepository;
    private final PersonalRepository personalRepository;
    private final PersonalMapper personalMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    @Override
    public LoginResponse login(LoginRequest request) {
        var username = request.username().toLowerCase(Locale.ROOT);
        var at = username.lastIndexOf('@');
        if (at < 0 || !username.endsWith(".com"))
            throw loginFailed("<malformed, length " + username.length() + ">", "malformed_username", new BadRequestException("auth.invalidCredentials"));
        var clinic = clinicRepository.findBySlug(username.substring(at + 1, username.length() - ".com".length()))
                .orElseThrow(() -> loginFailed(username, "unknown_clinic", new UnauthorizedException("auth.invalidCredentials")));
        var previousTenant = TenantContext.current();
        try {
            TenantContext.setCurrentTenant(clinic.getId(), clinic.getSlug());
            var personal = personalRepository.findByUsername(username)
                    .orElseThrow(() -> loginFailed(username, "unknown_user", new UnauthorizedException("auth.invalidCredentials")));
            if (!personal.isActive())
                throw loginFailed(username, "inactive_user", new UnauthorizedException("auth.userNotActive"));
            if (!passwordEncoder.matches(request.password(), personal.getPassword()))
                throw loginFailed(username, "bad_password", new UnauthorizedException("auth.invalidCredentials"));
            if (clinic.getStatus() != ClinicStatus.ACTIVE)
                throw loginFailed(username, "clinic_not_active", new ForbiddenException("auth.clinicNotActivated"));
            log.info("Login succeeded for user {} in clinic {}", personal.getId(), clinic.getId());
            return new LoginResponse(jwtUtil.generate(personal.getId(), personal.getRole().name(), clinic, personal.getName()));
        } finally {
            if (previousTenant == null)
                TenantContext.clear();
            else
                TenantContext.setCurrentTenant(previousTenant.clinicId(), previousTenant.clinicSlug());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public PersonalResponse findProfile(Long userId) {
        return personalRepository.findById(userId)
                .map(personalMapper::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("personal.notFound", userId));
    }

    private static ApiException loginFailed(String username, String reason, ApiException exception) {
        log.atWarn()
                .addKeyValue("reason", reason)
                .log("Login failed for {}: {}", sanitizeUsername(username), reason);
        return exception;
    }

    private static String sanitizeUsername(String username) {
        var clean = CONTROL_CHARS.matcher(username).replaceAll("");
        return clean.length() > MAX_LOGGED_USERNAME ? clean.substring(0, MAX_LOGGED_USERNAME) + "..." : clean;
    }
}
