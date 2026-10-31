package com.example.scheduler.service.impl;

import com.example.scheduler.config.tenant.TenantContext;
import com.example.scheduler.dto.patient.PatientResponse;
import com.example.scheduler.dto.personal.AssignAndRemoveRequest;
import com.example.scheduler.dto.personal.PersonalProfileUpdateRequest;
import com.example.scheduler.dto.personal.PersonalRequest;
import com.example.scheduler.dto.personal.PersonalResponse;
import com.example.scheduler.dto.personal.PersonalUpdateRequest;
import com.example.scheduler.entity.*;
import com.example.scheduler.enums.AppointmentStatus;
import com.example.scheduler.enums.ERole;
import com.example.scheduler.exception.BadRequestException;
import com.example.scheduler.exception.BusinessException;
import com.example.scheduler.exception.ForbiddenException;
import com.example.scheduler.exception.MessageKeys;
import com.example.scheduler.exception.ResourceNotFoundException;
import com.example.scheduler.mapper.PatientMapper;
import com.example.scheduler.mapper.PersonalMapper;
import com.example.scheduler.notification.StaffCreatedEvent;
import com.example.scheduler.repository.*;
import com.example.scheduler.service.PersonalService;
import com.example.scheduler.util.Utils;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PersonalServiceImpl implements PersonalService {
    private final PersonalRepository personalRepository;
    private final ClinicRepository clinicRepository;
    private final PatientRepository patientRepository;
    private final SpecialtyRepository specialtyRepository;
    private final PersonalMapper personalMapper;
    private final PatientMapper patientMapper;
    private final PasswordEncoder passwordEncoder;
    private final AppointmentRepository appointmentRepository;
    private final DoctorAvailabilityRepository doctorAvailabilityRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final Clock clock;

    @Override
    public Page<PersonalResponse> findAllDoctors(Long specialtyId, Boolean isActive, Pageable pageable) {
        if (specialtyId != null) getSpecialtyOrThrowById(specialtyId);
        var active = isActive == null ? Boolean.TRUE : isActive;
        return personalRepository
                .findAllDoctorsByFilters(specialtyId, active, pageable)
                .map(personalMapper::toResponse);
    }

    @Override
    public Page<PersonalResponse> findAllPersonal(Long specialtyId, Boolean isActive, ERole role, Pageable pageable) {
        if (specialtyId != null) getSpecialtyOrThrowById(specialtyId);
        return personalRepository
                .findAllByFilters(specialtyId, isActive, role, pageable)
                .map(personalMapper::toResponse);
    }

    @Override
    @Transactional
    public PersonalResponse createPersonal(PersonalRequest request) {
        var clinic = getCurrentClinic();
        var username = request.username() + Utils.usernameSuffix(clinic.getSlug());
        if (personalRepository.existsByUsername(username))
            throw new BusinessException(MessageKeys.PERSONAL_USERNAME_TAKEN, username);
        if (personalRepository.existsByCi(request.ci()))
            throw new BusinessException(MessageKeys.PERSONAL_ALREADY_EXISTS);
        if (personalRepository.existsByEmail(request.email()))
            throw new BadRequestException(MessageKeys.PERSONAL_EMAIL_ALREADY_EXISTS, request.email());
        var phoneNumber = Utils.normalizePhoneNumber(request.phoneNumber());
        if (phoneNumber != null && personalRepository.existsByPhoneNumber(phoneNumber))
            throw new BusinessException(MessageKeys.PERSONAL_PHONE_ALREADY_EXISTS, phoneNumber);
        var personal = Personal.builder()
                .username(username)
                .ci(request.ci())
                .name(request.name())
                .email(request.email())
                .password(passwordEncoder.encode(request.password()))
                .phoneNumber(phoneNumber)
                .role(request.role())
                .specialty(getSpecialtyOrThrowById(request.specialtyId()))
                .build();
        var saved = personalRepository.save(personal);
        eventPublisher.publishEvent(new StaffCreatedEvent(
                saved.getEmail(),
                saved.getName(),
                saved.getUsername(),
                request.password(),
                saved.getRole(),
                clinic.getName()));
        return personalMapper.toResponse(saved);
    }

    @Override
    public PersonalResponse findPersonalById(Long personalId) {
        var personal = personalRepository
                .findById(personalId)
                .orElseThrow(() -> new ResourceNotFoundException(MessageKeys.PERSONAL_NOT_FOUND, personalId));
        return personalMapper.toResponse(personal);
    }

    @Override
    @Transactional
    public PersonalResponse updatePersonalByAdmin(Long personalId, PersonalUpdateRequest request) {
        var personal = getPersonalOrThrowById(personalId);
        if (personal.getRole() == ERole.DOCTOR && request.role() == ERole.ASSISTANT) {
            var now = LocalDateTime.now(clock);
            if (appointmentRepository.existsByDoctorIdAndStatusNotAndStartTimeAfter(
                    personalId, AppointmentStatus.CANCELLED, now))
                throw new BusinessException(MessageKeys.PERSONAL_DOCTOR_HAS_UPCOMING_APPOINTMENTS);
            if (doctorAvailabilityRepository.existsByDoctorIdAndActiveTrue(personalId))
                throw new BusinessException(MessageKeys.PERSONAL_DOCTOR_HAS_ACTIVE_AVAILABILITIES);
        }
        updateContactData(personal, request.name(), request.email(), request.phoneNumber());
        if (personal.getRole() != ERole.ADMINISTRATOR) personal.setRole(request.role());
        personal.setSpecialty(getSpecialtyOrThrowById(request.specialtyId()));
        if (request.password() != null) personal.setPassword(passwordEncoder.encode(request.password()));
        return personalMapper.toResponse(personalRepository.save(personal));
    }

    @Override
    @Transactional
    public PersonalResponse updateOwnProfile(Long userId, PersonalProfileUpdateRequest request) {
        var personal = getPersonalOrThrowById(userId);
        if (request.newPassword() != null) {
            if (request.currentPassword() == null
                    || !passwordEncoder.matches(request.currentPassword(), personal.getPassword()))
                throw new BadRequestException(MessageKeys.PERSONAL_CURRENT_PASSWORD_INCORRECT);
            personal.setPassword(passwordEncoder.encode(request.newPassword()));
        }
        updateContactData(personal, request.name(), request.email(), request.phoneNumber());
        return personalMapper.toResponse(personalRepository.save(personal));
    }

    private void updateContactData(Personal personal, String name, String email, String rawPhoneNumber) {
        if (!personal.getEmail().equals(email) && personalRepository.existsByEmail(email))
            throw new BadRequestException(MessageKeys.PERSONAL_EMAIL_ALREADY_EXISTS, email);
        var phoneNumber = Utils.normalizePhoneNumber(rawPhoneNumber);
        if (phoneNumber != null
                && !Objects.equals(personal.getPhoneNumber(), phoneNumber)
                && personalRepository.existsByPhoneNumber(phoneNumber))
            throw new BusinessException(MessageKeys.PERSONAL_PHONE_ALREADY_EXISTS, phoneNumber);
        personal.setName(name);
        personal.setEmail(email);
        personal.setPhoneNumber(phoneNumber);
    }

    @Override
    @Transactional
    public void deactivatePersonalById(Long personalId, Long userId) {
        if (personalId.equals(userId)) throw new BusinessException(MessageKeys.PERSONAL_CANNOT_DEACTIVATE_SELF);
        var personal = getPersonalOrThrowById(personalId);
        personal.setActive(false);
        personalRepository.save(personal);
    }

    @Override
    @Transactional
    public PersonalResponse activatePersonalById(Long personalId) {
        var personal = getPersonalOrThrowById(personalId);
        personal.setActive(true);
        return personalMapper.toResponse(personalRepository.save(personal));
    }

    @Override
    @Transactional
    public void assignPatient(AssignAndRemoveRequest request, Long userId, String role) {
        var doctor = getDoctorPatientsOrThrowById(request.doctorId());
        var patient = getPatientOrThrowById(request.patientId());
        verifyDoctorPermission(role, doctor.getId(), userId);
        if (!doctor.getPatients().contains(patient)) {
            doctor.getPatients().add(patient);
            personalRepository.save(doctor);
        }
    }

    @Override
    @Transactional
    public void removePatient(AssignAndRemoveRequest request, Long userId, String role) {
        var doctor = getDoctorPatientsOrThrowById(request.doctorId());
        var patient = getPatientOrThrowById(request.patientId());
        verifyDoctorPermission(role, doctor.getId(), userId);
        if (doctor.getPatients().contains(patient)) {
            doctor.getPatients().remove(patient);
            personalRepository.save(doctor);
        }
    }

    @Override
    public List<PatientResponse> getPatientsOfDoctor(Long doctorId, Long userId, String role) {
        verifyDoctorPermission(role, doctorId, userId);
        var doctor = getDoctorPatientsOrThrowById(doctorId);
        return patientMapper.toResponseList(doctor.getPatients());
    }

    private Clinic getCurrentClinic() {
        var clinicId = TenantContext.getCurrentClinicId();
        return clinicRepository
                .findById(clinicId)
                .orElseThrow(() -> new ResourceNotFoundException(MessageKeys.CLINIC_NOT_FOUND, clinicId));
    }

    private Personal getPersonalOrThrowById(Long personalId) {
        return personalRepository
                .findByIdForUpdate(personalId)
                .orElseThrow(() -> new ResourceNotFoundException(MessageKeys.PERSONAL_NOT_FOUND, personalId));
    }

    private Personal getDoctorPatientsOrThrowById(Long personalId) {
        return personalRepository
                .findDoctorPatientsByIdAndRole(personalId, ERole.DOCTOR)
                .orElseThrow(() -> new ResourceNotFoundException(MessageKeys.PERSONAL_NOT_FOUND, personalId));
    }

    private Specialty getSpecialtyOrThrowById(Long specialtyId) {
        if (specialtyId == null) {
            return null;
        }
        return specialtyRepository
                .findById(specialtyId)
                .orElseThrow(() -> new ResourceNotFoundException(MessageKeys.SPECIALTY_NOT_FOUND, specialtyId));
    }

    private Patient getPatientOrThrowById(Long patientId) {
        return patientRepository
                .findById(patientId)
                .orElseThrow(() -> new ResourceNotFoundException(MessageKeys.PATIENT_NOT_FOUND, patientId));
    }

    private void verifyDoctorPermission(String role, Long accountId, Long userId) {
        if (ERole.DOCTOR.name().equals(role) && !accountId.equals(userId))
            throw new ForbiddenException(MessageKeys.COMMON_FORBIDDEN);
    }
}
