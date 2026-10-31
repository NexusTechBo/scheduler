package com.example.scheduler.service;

import com.example.scheduler.dto.login.LoginRequest;
import com.example.scheduler.dto.login.LoginResponse;
import com.example.scheduler.dto.personal.PersonalResponse;

public interface AuthService {
    LoginResponse login(LoginRequest request);

    PersonalResponse findProfile(Long userId);
}
