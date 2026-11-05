package com.example.scheduler.service.impl;

import com.example.scheduler.dto.personal.DoctorModeRequest;
import com.example.scheduler.dto.personal.PersonalProfileUpdateRequest;
import com.example.scheduler.dto.personal.PersonalRequest;
import com.example.scheduler.dto.personal.PersonalResponse;
import com.example.scheduler.dto.personal.PersonalUpdateRequest;
import com.example.scheduler.entity.Personal;
import com.example.scheduler.entity.Specialty;
import com.example.scheduler.enums.AppointmentStatus;
import com.example.scheduler.enums.ERole;
import com.example.scheduler.exception.BadRequestException;
import com.example.scheduler.exception.BusinessException;
import com.example.scheduler.exception.ForbiddenException;
import com.example.scheduler.exception.MessageKeys;
import com.example.scheduler.exception.ResourceNotFoundException;
import com.example.scheduler.mapper.PersonalMapper;
import com.example.scheduler.notification.event.StaffCreatedEvent;
import com.example.scheduler.repository.AppointmentRepository;
import com.example.scheduler.repository.DoctorAvailabilityRepository;
import com.example.scheduler.repository.PersonalRepository;
import com.example.scheduler.repository.SpecialtyRepository;
import com.example.scheduler.service.AppointmentService;
import com.example.scheduler.service.PersonalService;
import com.example.scheduler.util.Utils;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PersonalServiceImpl implements PersonalService {
    private final PersonalRepository personalRepository;
    private final SpecialtyRepository specialtyRepository;
    private final CurrentClinicProvider currentClinicProvider;
    private final PersonalMapper personalMapper;
    private final PasswordEncoder passwordEncoder;
    private final AppointmentService appointmentService;
    private final AppointmentRepository appointmentRepository;
    private final DoctorAvailabilityRepository doctorAvailabilityRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final Clock clock;

    @Override
    public Page<PersonalResponse> findAllDoctors(Long specialtyId, Boolean isActive, Pageable pageable) {
        verifySpecialtyExists(specialtyId);
        var active = isActive == null ? Boolean.TRUE : isActive;
        return personalRepository
                .findAllDoctorsByFilters(specialtyId, active, pageable)
                .map(personalMapper::toResponse);
    }

    @Override
    public Page<PersonalResponse> findAllPersonal(Long specialtyId, Boolean isActive, ERole role, Pageable pageable) {
        verifySpecialtyExists(specialtyId);
        return personalRepository
                .findAllByFilters(specialtyId, isActive, role, role == ERole.DOCTOR, pageable)
                .map(personalMapper::toResponse);
    }

    @Override
    @Transactional
    public PersonalResponse createPersonal(PersonalRequest request) {
        var clinic = currentClinicProvider.getCurrentClinic();
        var username = request.username() + Utils.usernameSuffix(clinic.getSlug());
        if (personalRepository.existsByUsername(username))
            throw new BusinessException(MessageKeys.PERSONAL_USERNAME_TAKEN, username);
        if (personalRepository.existsByCi(request.ci()))
            throw new BusinessException(MessageKeys.PERSONAL_ALREADY_EXISTS);
        var email = Utils.normalizeEmail(request.email());
        if (personalRepository.existsByEmailIgnoreCase(email))
            throw new BusinessException(MessageKeys.PERSONAL_EMAIL_ALREADY_EXISTS, email);
        var phoneNumber = Utils.normalizePhoneNumber(request.phoneNumber());
        if (phoneNumber != null && personalRepository.existsByPhoneNumber(phoneNumber))
            throw new BusinessException(MessageKeys.PERSONAL_PHONE_ALREADY_EXISTS, phoneNumber);
        var personal = Personal.builder()
                .username(username)
                .ci(request.ci())
                .name(request.name())
                .email(email)
                .password(passwordEncoder.encode(request.password()))
                .phoneNumber(phoneNumber)
                .role(request.role())
                .specialty(getSpecialtyOrNull(request.specialtyId()))
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
        if (personal.getRole() == ERole.DOCTOR && request.role() == ERole.ASSISTANT)
            verifyCanStopBeingDoctor(personalId);
        if (personal.isAlsoDoctor() && request.specialtyId() == null)
            throw new BadRequestException(MessageKeys.PERSONAL_DOCTOR_SPECIALTY_REQUIRED);
        updateContactData(personal, request.name(), request.email(), request.phoneNumber());
        if (personal.getRole() != ERole.ADMINISTRATOR) personal.setRole(request.role());
        personal.setSpecialty(getSpecialtyOrNull(request.specialtyId()));
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

    @Override
    @Transactional
    public void updateDoctorMode(Long userId, DoctorModeRequest request) {
        var personal = getPersonalOrThrowById(userId);
        if (personal.getRole() != ERole.ADMINISTRATOR) throw new ForbiddenException(MessageKeys.COMMON_FORBIDDEN);
        if (request.enabled()) {
            if (request.specialtyId() == null)
                throw new BadRequestException(MessageKeys.PERSONAL_DOCTOR_SPECIALTY_REQUIRED);
            personal.setSpecialty(getSpecialtyOrThrow(request.specialtyId()));
            personal.setAlsoDoctor(true);
        } else if (personal.isAlsoDoctor()) {
            verifyCanStopBeingDoctor(userId);
            personal.setAlsoDoctor(false);
            personal.setSpecialty(null);
        }
        var saved = personalRepository.saveAndFlush(personal);
        log.info("Doctor mode of administrator {} set to {}", saved.getId(), saved.isAlsoDoctor());
    }

    @Override
    @Transactional
    public void deactivatePersonalById(Long personalId, Long userId) {
        if (personalId.equals(userId)) throw new BusinessException(MessageKeys.PERSONAL_CANNOT_DEACTIVATE_SELF);
        var personal = getPersonalOrThrowById(personalId);
        personal.setActive(false);
        personalRepository.save(personal);
        if (personal.isDoctor()) appointmentService.cancelUpcomingAppointmentsOfDeactivatedDoctor(personalId);
    }

    @Override
    @Transactional
    public PersonalResponse activatePersonalById(Long personalId) {
        var personal = getPersonalOrThrowById(personalId);
        personal.setActive(true);
        return personalMapper.toResponse(personalRepository.save(personal));
    }

    private void updateContactData(Personal personal, String name, String rawEmail, String rawPhoneNumber) {
        var email = Utils.normalizeEmail(rawEmail);
        if (!personal.getEmail().equalsIgnoreCase(email) && personalRepository.existsByEmailIgnoreCase(email))
            throw new BusinessException(MessageKeys.PERSONAL_EMAIL_ALREADY_EXISTS, email);
        var phoneNumber = Utils.normalizePhoneNumber(rawPhoneNumber);
        if (phoneNumber != null
                && !Objects.equals(personal.getPhoneNumber(), phoneNumber)
                && personalRepository.existsByPhoneNumber(phoneNumber))
            throw new BusinessException(MessageKeys.PERSONAL_PHONE_ALREADY_EXISTS, phoneNumber);
        personal.setName(name);
        personal.setEmail(email);
        personal.setPhoneNumber(phoneNumber);
    }

    private Personal getPersonalOrThrowById(Long personalId) {
        return personalRepository
                .findByIdForUpdate(personalId)
                .orElseThrow(() -> new ResourceNotFoundException(MessageKeys.PERSONAL_NOT_FOUND, personalId));
    }

    private Specialty getSpecialtyOrThrow(Long specialtyId) {
        return specialtyRepository
                .findById(specialtyId)
                .orElseThrow(() -> new ResourceNotFoundException(MessageKeys.SPECIALTY_NOT_FOUND, specialtyId));
    }

    private Specialty getSpecialtyOrNull(Long specialtyId) {
        return specialtyId == null ? null : getSpecialtyOrThrow(specialtyId);
    }

    private void verifySpecialtyExists(Long specialtyId) {
        if (specialtyId != null && !specialtyRepository.existsById(specialtyId))
            throw new ResourceNotFoundException(MessageKeys.SPECIALTY_NOT_FOUND, specialtyId);
    }

    private void verifyCanStopBeingDoctor(Long doctorId) {
        if (appointmentRepository.existsByDoctorIdAndStatusNotAndStartTimeAfter(
                doctorId, AppointmentStatus.CANCELLED, LocalDateTime.now(clock)))
            throw new BusinessException(MessageKeys.PERSONAL_DOCTOR_HAS_UPCOMING_APPOINTMENTS);
        if (doctorAvailabilityRepository.existsByDoctorIdAndActiveTrue(doctorId))
            throw new BusinessException(MessageKeys.PERSONAL_DOCTOR_HAS_ACTIVE_AVAILABILITIES);
    }
}
