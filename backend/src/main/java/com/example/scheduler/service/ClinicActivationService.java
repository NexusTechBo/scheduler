package com.example.scheduler.service;

import com.example.scheduler.dto.clinic.ActivateClinicRequest;
import com.example.scheduler.dto.clinic.ClinicActivatedResponse;
import com.example.scheduler.dto.clinic.ResendActivationRequest;
import com.example.scheduler.entity.Clinic;

public interface ClinicActivationService {
    /** Stores a new activation token for the clinic and emails it after commit. Requires an active transaction. */
    void issueActivationToken(Clinic clinic, String adminName, String adminEmail);
    ClinicActivatedResponse activateClinic(ActivateClinicRequest request);
    void resendActivation(ResendActivationRequest request);
}
