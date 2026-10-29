package com.example.scheduler.service;

import com.example.scheduler.dto.clinic.ActivateClinicRequest;
import com.example.scheduler.dto.clinic.ClinicActivatedResponse;
import com.example.scheduler.dto.clinic.ResendActivationRequest;
import com.example.scheduler.entity.Clinic;

public interface ClinicActivationService {
    void issueActivationToken(Clinic clinic, String adminName, String adminEmail);
    ClinicActivatedResponse activateClinic(ActivateClinicRequest request);
    void resendActivation(ResendActivationRequest request);
}
