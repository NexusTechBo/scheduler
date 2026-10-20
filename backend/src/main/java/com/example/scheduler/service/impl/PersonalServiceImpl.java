package com.example.scheduler.service.impl;

import com.example.scheduler.config.tenant.TenantContext;
import com.example.scheduler.dto.patient.PatientResponse;
import com.example.scheduler.dto.personal.AssignAndRemoveRequest;
import com.example.scheduler.dto.personal.PersonalRequest;
import com.example.scheduler.dto.personal.PersonalUpdateRequest;
import com.example.scheduler.dto.personal.PersonalProfileUpdateRequest;
import com.example.scheduler.dto.personal.PersonalResponse;
import com.example.scheduler.entity.*;
import com.example.scheduler.enums.AppointmentStatus;
import com.example.scheduler.enums.ERole;
import com.example.scheduler.exception.BadRequestException;
import com.example.scheduler.exception.BusinessException;
import com.example.scheduler.exception.ForbiddenException;
import com.example.scheduler.exception.ResourceNotFoundException;
import com.example.scheduler.mapper.PatientMapper;
import com.example.scheduler.mapper.PersonalMapper;
import com.example.scheduler.repository.*;
import com.example.scheduler.service.PersonalService;
import com.example.scheduler.util.Utils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

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
    private final Clock clock;

    @Override
    public Page<PersonalResponse> findAllDoctors(Long specialtyId, Boolean isActive, Pageable pageable) {
        if(specialtyId != null) getSpecialtyOrThrowById(specialtyId);
        var active = isActive == null ? Boolean.TRUE : isActive;
        return personalRepository.findAllDoctorsByFilters(specialtyId, active, pageable)
                .map(personalMapper::toResponse);
    }

    @Override
    public Page<PersonalResponse> findAllPersonal(Long specialtyId, Boolean isActive, ERole role, Pageable pageable) {
        if(specialtyId != null) getSpecialtyOrThrowById(specialtyId);
        return personalRepository.findAllByFilters(specialtyId, isActive, role, pageable)
                .map(personalMapper::toResponse);
    }

    @Override
    @Transactional
    public PersonalResponse createPersonal(PersonalRequest request) {
        var username = request.username() + getCurrentClinic().getSubdomain();
        if (personalRepository.existsByUsername(username))
            throw new BusinessException("The username " + username + " is already taken");
        if (personalRepository.existsByCi(request.ci()))
            throw new BusinessException("This personal already exists");
        if (personalRepository.existsByEmail(request.email()))
            throw new BadRequestException("Personal with email " + request.email() + " already exists");
        var phoneNumber = Utils.normalizePhoneNumber(request.phoneNumber());
        if (phoneNumber != null && personalRepository.existsByPhoneNumber(phoneNumber))
            throw new BusinessException("Personal with phone number " + phoneNumber + " already exists");
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
        return personalMapper.toResponse(personalRepository.save(personal));
    }

    @Override
    public PersonalResponse findPersonalById(Long personalId) {
        return personalMapper.toResponse(getPersonalOrThrowById(personalId));
    }

    @Override
    @Transactional
    public PersonalResponse updatePersonalByAdmin(Long personalId, PersonalUpdateRequest request) {
        var personal = personalRepository.findByIdForUpdate(personalId)
                .orElseThrow(() -> new ResourceNotFoundException("Personal not found with id: " + personalId));
        if (personal.getRole() == ERole.DOCTOR && request.role() == ERole.ASSISTANT) {
            var now = LocalDateTime.now(clock);
            if (appointmentRepository.existsByDoctorIdAndStatusNotAndStartTimeAfter(personalId, AppointmentStatus.CANCELLED, now))
                throw new BusinessException("The doctor has upcoming appointments; cancel them before changing the role");
            if (doctorAvailabilityRepository.existsByDoctorIdAndActiveTrue(personalId))
                throw new BusinessException("The doctor has active availabilities; remove them before changing the role");
        }
        updateContactData(personal, request.name(), request.email(), request.phoneNumber());
        personal.setRole(request.role());
        personal.setSpecialty(getSpecialtyOrThrowById(request.specialtyId()));
        if (request.password() != null)
            personal.setPassword(passwordEncoder.encode(request.password()));
        return personalMapper.toResponse(personalRepository.save(personal));
    }

    @Override
    @Transactional
    public PersonalResponse updateOwnProfile(Long userId, PersonalProfileUpdateRequest request) {
        var personal = getPersonalOrThrowById(userId);
        if (request.newPassword() != null) {
            if (request.currentPassword() == null || !passwordEncoder.matches(request.currentPassword(), personal.getPassword()))
                throw new BadRequestException("The current password is incorrect");
            personal.setPassword(passwordEncoder.encode(request.newPassword()));
        }
        updateContactData(personal, request.name(), request.email(), request.phoneNumber());
        return personalMapper.toResponse(personalRepository.save(personal));
    }

    private void updateContactData(Personal personal, String name, String email, String rawPhoneNumber) {
        if (!Objects.equals(personal.getEmail(), email) && personalRepository.existsByEmail(email))
            throw new BadRequestException("Personal with email " + email + " already exists");
        var phoneNumber = Utils.normalizePhoneNumber(rawPhoneNumber);
        if (phoneNumber != null && !Objects.equals(personal.getPhoneNumber(), phoneNumber) && personalRepository.existsByPhoneNumber(phoneNumber))
            throw new BusinessException("Personal with phone number " + phoneNumber + " already exists");
        personal.setName(name);
        personal.setEmail(email);
        personal.setPhoneNumber(phoneNumber);
    }

    @Override
    @Transactional
    public void deactivatePersonalById(Long personalId) {
        var personal = getPersonalOrThrowById(personalId);
        personal.setActive(false);
        personalRepository.save(personal);
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
        var clinicId = Long.valueOf(TenantContext.getCurrentTenant().substring("clinic_".length()));
        return clinicRepository.findById(clinicId)
                .orElseThrow(() -> new ResourceNotFoundException("Clinic not found with id: " + clinicId));
    }

    private Personal getPersonalOrThrowById(Long personalId) {
        return personalRepository.findById(personalId)
                .orElseThrow(() -> new ResourceNotFoundException("Personal not found with id: " + personalId));
    }

    private Personal getDoctorPatientsOrThrowById(Long personalId) {
        return personalRepository.findDoctorPatientsByIdAndRole(personalId, ERole.DOCTOR)
                .orElseThrow(() -> new ResourceNotFoundException("Personal not found with id: " + personalId));
    }

    private Specialty getSpecialtyOrThrowById(Long specialtyId) {
        if(specialtyId == null){
            return null;
        }
        return  specialtyRepository.findById(specialtyId)
                .orElseThrow(() -> new ResourceNotFoundException("Specialty not found with id: " + specialtyId));
    }

    private Patient getPatientOrThrowById(Long patientId) {
        return patientRepository.findById(patientId)
                .orElseThrow(() -> new ResourceNotFoundException("Patient not found with id: " + patientId));
    }

    public void verifyDoctorPermission(String role, Long accountId, Long userId) {
        if (role.equals(ERole.DOCTOR.name()) && !accountId.equals(userId))
            throw new ForbiddenException("Not authorize to do this");
    }
}
