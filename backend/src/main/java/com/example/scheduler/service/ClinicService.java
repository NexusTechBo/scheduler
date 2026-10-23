package com.example.scheduler.service;

import com.example.scheduler.dto.clinic.ClinicCreatedResponse;
import com.example.scheduler.dto.clinic.ClinicRequest;

public interface ClinicService {
    ClinicCreatedResponse createClinic(ClinicRequest request);
}
