package com.example.scheduler.service.impl;

import com.example.scheduler.config.tenant.TenantContext;
import com.example.scheduler.dto.DoctorAvailability.DoctorAvailabilitySlotsResponse;
import com.example.scheduler.dto.appointment.AppointmentRequest;
import com.example.scheduler.dto.guest.GuestAppointmentRequest;
import com.example.scheduler.dto.guest.GuestAppointmentResponse;
import com.example.scheduler.dto.guest.GuestClinicResponse;
import com.example.scheduler.dto.guest.GuestDoctorResponse;
import com.example.scheduler.dto.patient.PatientRequest;
import com.example.scheduler.enums.ERole;
import com.example.scheduler.exception.ApiException;
import com.example.scheduler.exception.BusinessException;
import com.example.scheduler.exception.ResourceNotFoundException;
import com.example.scheduler.mapper.GuestMapper;
import com.example.scheduler.repository.ClinicRepository;
import com.example.scheduler.repository.PatientRepository;
import com.example.scheduler.repository.PersonalRepository;
import com.example.scheduler.service.AppointmentService;
import com.example.scheduler.service.DoctorAvailabilityService;
import com.example.scheduler.service.GuestService;
import com.example.scheduler.service.PatientService;
import com.example.scheduler.util.Utils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GuestServiceImpl implements GuestService {
    private static final String ERR_CANNOT_BOOK = "guest.cannotBook";

    private final ClinicRepository clinicRepository;
    private final PersonalRepository personalRepository;
    private final PatientRepository patientRepository;
    private final PatientService patientService;
    private final AppointmentService appointmentService;
    private final DoctorAvailabilityService doctorAvailabilityService;
    private final GuestMapper guestMapper;

    @Override
    public GuestClinicResponse findCurrentClinic() {
        var clinicId = TenantContext.getCurrentClinicId();
        return clinicRepository.findById(clinicId)
                .map(c -> new GuestClinicResponse(c.getName(), c.getSlug()))
                .orElseThrow(() -> new ResourceNotFoundException("clinic.notFound", clinicId));
    }

    @Override
    public List<GuestDoctorResponse> findAllActiveDoctors(Long specialtyId) {
        return guestMapper.toDoctorResponseList(personalRepository.findAllDoctorsActive(specialtyId));
    }

    @Override
    public DoctorAvailabilitySlotsResponse getDoctorAvailableSlots(Long doctorId, LocalDate date) {
        verifyActiveDoctor(doctorId);
        return doctorAvailabilityService.getDoctorAvailableSlots(doctorId, date);
    }

    @Override
    @Transactional
    public GuestAppointmentResponse bookAppointment(GuestAppointmentRequest request) {
        verifyActiveDoctor(request.doctorId());
        appointmentService.verifySlotBookable(request.doctorId(), request.startTime());
        var patientId = resolvePatientId(request.patient());
        var appointment = appointmentService.bookAppointment(
                new AppointmentRequest(request.doctorId(), patientId, request.startTime()));
        return guestMapper.toAppointmentResponse(appointment);
    }

    private Long resolvePatientId(PatientRequest patient) {
        var existing = patientRepository.findByCi(patient.ci());
        if (existing.isPresent()) {
            var stored = existing.get();
            if (!stored.isActive() || !stored.getEmail().strip().equalsIgnoreCase(patient.email()))
                throw new BusinessException(ERR_CANNOT_BOOK);
            return stored.getId();
        }
        var phoneNumber = Utils.normalizePhoneNumber(patient.phoneNumber());
        if (patientRepository.existsByEmailIgnoreCase(patient.email())
                || (phoneNumber != null && patientRepository.existsByPhoneNumber(phoneNumber)))
            throw new BusinessException(ERR_CANNOT_BOOK);
        try {
            return patientService.createPatient(patient).id();
        } catch (ApiException | DataIntegrityViolationException e) {
            log.info("Guest patient creation rejected: {}", e.getMessage());
            throw new BusinessException(ERR_CANNOT_BOOK);
        }
    }

    private void verifyActiveDoctor(Long doctorId) {
        if (personalRepository.findByIdAndRoleAndActiveIsTrue(doctorId, ERole.DOCTOR).isEmpty())
            throw new ResourceNotFoundException("personal.doctorNotFound", doctorId);
    }
}
