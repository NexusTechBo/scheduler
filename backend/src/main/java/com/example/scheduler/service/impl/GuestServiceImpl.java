package com.example.scheduler.service.impl;

import com.example.scheduler.config.tenant.TenantContext;
import com.example.scheduler.dto.appointment.AppointmentRequest;
import com.example.scheduler.dto.doctorAvailability.DoctorAvailabilitySlotsResponse;
import com.example.scheduler.dto.guest.GuestAppointmentRequest;
import com.example.scheduler.dto.guest.GuestAppointmentResponse;
import com.example.scheduler.dto.guest.GuestClinicResponse;
import com.example.scheduler.dto.guest.GuestDoctorResponse;
import com.example.scheduler.dto.guest.GuestNewPatientRequest;
import com.example.scheduler.dto.guest.GuestPatientRequest;
import com.example.scheduler.dto.patient.PatientRequest;
import com.example.scheduler.entity.Patient;
import com.example.scheduler.entity.Personal;
import com.example.scheduler.enums.ERole;
import com.example.scheduler.exception.ApiException;
import com.example.scheduler.exception.BusinessException;
import com.example.scheduler.exception.MessageKeys;
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
import java.time.LocalDate;
import java.util.List;
import java.util.function.Supplier;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GuestServiceImpl implements GuestService {
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
        return clinicRepository
                .findById(clinicId)
                .map(c -> new GuestClinicResponse(c.getName(), c.getSlug()))
                .orElseThrow(() -> new ResourceNotFoundException(MessageKeys.CLINIC_NOT_FOUND, clinicId));
    }

    @Override
    public List<GuestDoctorResponse> findAllActiveDoctors(Long specialtyId) {
        return guestMapper.toDoctorResponseList(personalRepository.findAllDoctorsActive(specialtyId));
    }

    @Override
    public DoctorAvailabilitySlotsResponse getDoctorAvailableSlots(Long doctorId, LocalDate date) {
        getDoctorOrThrowById(doctorId);
        return doctorAvailabilityService.getDoctorAvailableSlots(doctorId, date);
    }

    @Override
    @Transactional
    public GuestAppointmentResponse bookAppointment(GuestAppointmentRequest<GuestPatientRequest> request) {
        return book(request, () -> resolvePatientId(request.patient()));
    }

    @Override
    @Transactional
    public GuestAppointmentResponse bookAppointmentForNewPatient(
            GuestAppointmentRequest<GuestNewPatientRequest> request) {
        return book(request, () -> resolveNewPatientId(request.patient()));
    }

    private GuestAppointmentResponse book(GuestAppointmentRequest<?> request, Supplier<Long> patientId) {
        var doctor = getDoctorOrThrowById(request.doctorId());
        appointmentService.verifySlotBookable(doctor, request.startTime());
        var appointment = appointmentService.bookGuestAppointment(
                new AppointmentRequest(request.doctorId(), patientId.get(), request.startTime()));
        return guestMapper.toAppointmentResponse(appointment);
    }

    private Long resolvePatientId(GuestPatientRequest patient) {
        return getActivePatientByCiAndPhoneNumberOrThrow(
                        patient.ci(), Utils.normalizePhoneNumber(patient.phoneNumber()))
                .getId();
    }

    private Long resolveNewPatientId(GuestNewPatientRequest patient) {
        var phoneNumber = Utils.normalizePhoneNumber(patient.phoneNumber());
        var email = Utils.blankToNull(patient.email());
        var ci = patient.ci();
        if (patientRepository.existsByCiOrPhoneNumberOrEmailIgnoreCase(ci, phoneNumber, email))
            throw new BusinessException(MessageKeys.GUEST_CANNOT_BOOK);
        try {
            return patientService
                    .createPatient(new PatientRequest(ci, patient.name(), email, phoneNumber))
                    .id();
        } catch (ApiException | DataIntegrityViolationException e) {
            log.info(
                    "Guest patient creation rejected: {}",
                    e instanceof ApiException api
                            ? api.getMessageKey()
                            : e.getClass().getSimpleName());
            throw new BusinessException(MessageKeys.GUEST_CANNOT_BOOK);
        }
    }

    private Personal getDoctorOrThrowById(Long doctorId) {
        return personalRepository
                .findByIdAndRoleAndActiveIsTrue(doctorId, ERole.DOCTOR)
                .orElseThrow(() -> new ResourceNotFoundException(MessageKeys.PERSONAL_DOCTOR_NOT_FOUND, doctorId));
    }

    private Patient getActivePatientByCiAndPhoneNumberOrThrow(String ci, String phoneNumber) {
        return patientRepository
                .findByCiAndPhoneNumberAndActiveIsTrue(ci, phoneNumber)
                .orElseThrow(() -> new BusinessException(MessageKeys.GUEST_CANNOT_BOOK));
    }
}
