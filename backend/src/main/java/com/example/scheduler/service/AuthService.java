package com.example.scheduler.service;

import com.example.scheduler.dto.account.ProfileResponse;
import com.example.scheduler.dto.login.LoginRequest;
import com.example.scheduler.dto.login.LoginResponse;

public interface AuthService {
    LoginResponse login(LoginRequest request);
    ProfileResponse findProfile(Long userId, String role);
}
