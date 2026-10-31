package com.example.scheduler.service.impl;

import com.example.scheduler.config.tenant.TenantContext;
import com.example.scheduler.dto.login.LoginRequest;
import com.example.scheduler.dto.login.LoginResponse;
import com.example.scheduler.dto.personal.PersonalResponse;
import com.example.scheduler.enums.ClinicStatus;
import com.example.scheduler.exception.ApiException;
import com.example.scheduler.exception.BadRequestException;
import com.example.scheduler.exception.ForbiddenException;
import com.example.scheduler.exception.MessageKeys;
import com.example.scheduler.exception.ResourceNotFoundException;
import com.example.scheduler.exception.UnauthorizedException;
import com.example.scheduler.mapper.PersonalMapper;
import com.example.scheduler.repository.ClinicRepository;
import com.example.scheduler.repository.PersonalRepository;
import com.example.scheduler.security.JwtUtil;
import com.example.scheduler.service.AuthService;
import com.example.scheduler.util.LogUtils;
import com.example.scheduler.util.Utils;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
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
        if (at < 0 || !username.endsWith(Utils.USERNAME_DOMAIN))
            throw loginFailed(
                    "<malformed, length " + username.length() + ">",
                    "malformed_username",
                    new BadRequestException(MessageKeys.AUTH_INVALID_CREDENTIALS));
        var clinic = clinicRepository
                .findBySlug(username.substring(at + 1, username.length() - Utils.USERNAME_DOMAIN.length()))
                .orElseThrow(() -> loginFailed(
                        username, "unknown_clinic", new UnauthorizedException(MessageKeys.AUTH_INVALID_CREDENTIALS)));
        var previousTenant = TenantContext.current();
        try {
            TenantContext.setCurrentTenant(clinic.getId(), clinic.getSlug());
            var personal = personalRepository
                    .findByUsername(username)
                    .orElseThrow(() -> loginFailed(
                            username, "unknown_user", new UnauthorizedException(MessageKeys.AUTH_INVALID_CREDENTIALS)));
            if (!personal.isActive())
                throw loginFailed(
                        username, "inactive_user", new UnauthorizedException(MessageKeys.AUTH_USER_NOT_ACTIVE));
            if (!passwordEncoder.matches(request.password(), personal.getPassword()))
                throw loginFailed(
                        username, "bad_password", new UnauthorizedException(MessageKeys.AUTH_INVALID_CREDENTIALS));
            if (clinic.getStatus() != ClinicStatus.ACTIVE)
                throw loginFailed(
                        username, "clinic_not_active", new ForbiddenException(MessageKeys.AUTH_CLINIC_NOT_ACTIVATED));
            log.info("Login succeeded for user {} in clinic {}", personal.getId(), clinic.getId());
            return new LoginResponse(
                    jwtUtil.generate(personal.getId(), personal.getRole().name(), clinic, personal.getName()));
        } finally {
            TenantContext.restore(previousTenant);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public PersonalResponse findProfile(Long userId) {
        return personalRepository
                .findById(userId)
                .map(personalMapper::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException(MessageKeys.PERSONAL_NOT_FOUND, userId));
    }

    private static ApiException loginFailed(String username, String reason, ApiException exception) {
        log.atWarn()
                .addKeyValue("reason", reason)
                .log("Login failed for {}: {}", LogUtils.sanitizeUsername(username), reason);
        return exception;
    }
}
