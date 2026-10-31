package com.example.scheduler.service.impl;

import com.example.scheduler.dto.doctoravailability.DoctorAvailabilityRequest;
import com.example.scheduler.dto.doctoravailability.DoctorAvailabilityResponse;
import com.example.scheduler.dto.doctoravailability.DoctorAvailabilitySlotsResponse;
import com.example.scheduler.dto.doctoravailability.SlotResponse;
import com.example.scheduler.entity.DoctorAvailability;
import com.example.scheduler.enums.AppointmentStatus;
import com.example.scheduler.enums.ERole;
import com.example.scheduler.exception.BusinessException;
import com.example.scheduler.exception.ForbiddenException;
import com.example.scheduler.exception.MessageKeys;
import com.example.scheduler.exception.ResourceNotFoundException;
import com.example.scheduler.mapper.DoctorAvailabilityMapper;
import com.example.scheduler.repository.AppointmentRepository;
import com.example.scheduler.repository.DoctorAvailabilityRepository;
import com.example.scheduler.repository.PersonalRepository;
import com.example.scheduler.repository.ScheduleExceptionRepository;
import com.example.scheduler.service.DoctorAvailabilityService;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DoctorAvailabilityServiceImpl implements DoctorAvailabilityService {
    private final DoctorAvailabilityRepository doctorAvailabilityRepository;
    private final ScheduleExceptionRepository scheduleExceptionRepository;
    private final AppointmentRepository appointmentRepository;
    private final PersonalRepository personalRepository;
    private final DoctorAvailabilityMapper mapper;
    private final Clock clock;

    @Override
    @Transactional
    public DoctorAvailabilityResponse addDoctorAvailability(Long doctorId, DoctorAvailabilityRequest request) {
        var doctor = personalRepository
                .findByIdForUpdate(doctorId)
                .orElseThrow(() -> new ResourceNotFoundException(MessageKeys.PERSONAL_DOCTOR_NOT_FOUND, doctorId));
        if (doctor.getRole() != ERole.DOCTOR || !doctor.isActive())
            throw new ForbiddenException(MessageKeys.PERSONAL_DOCTOR_ONLY_ACTIVE);
        var overlaps =
                doctorAvailabilityRepository
                        .findByDoctorIdAndDayOfWeekAndActiveTrue(doctorId, request.dayOfWeek())
                        .stream()
                        .anyMatch(a -> request.startTime().isBefore(a.getEndTime())
                                && request.endTime().isAfter(a.getStartTime()));
        if (overlaps) throw new BusinessException(MessageKeys.AVAILABILITY_OVERLAPS);
        var availability = DoctorAvailability.builder()
                .doctor(doctor)
                .dayOfWeek(request.dayOfWeek())
                .startTime(request.startTime())
                .endTime(request.endTime())
                .slotDurationMinutes(request.slotDurationMinutes())
                .build();
        return mapper.toResponse(doctorAvailabilityRepository.save(availability));
    }

    @Override
    public List<DoctorAvailabilityResponse> getDoctorAvailabilities(Long doctorId) {
        return mapper.toResponseList(doctorAvailabilityRepository.findByDoctorIdAndActiveTrue(doctorId));
    }

    @Override
    public DoctorAvailabilitySlotsResponse getDoctorAvailableSlots(Long doctorId, LocalDate date) {
        var now = LocalDateTime.now(clock);
        var dayOfWeek = date.getDayOfWeek();
        var isActiveDoctor = personalRepository
                .findByIdAndRoleAndActiveIsTrue(doctorId, ERole.DOCTOR)
                .isPresent();
        if (!isActiveDoctor || date.isBefore(now.toLocalDate()))
            return new DoctorAvailabilitySlotsResponse(date, doctorId, List.of());
        var availabilities = doctorAvailabilityRepository.findByDoctorIdAndDayOfWeekAndActiveTrue(doctorId, dayOfWeek);
        if (availabilities.isEmpty()) return new DoctorAvailabilitySlotsResponse(date, doctorId, List.of());
        var exceptions = scheduleExceptionRepository.findByDoctorIdAndDate(doctorId, date);
        var startOfDay = date.atStartOfDay();
        var endOfDay = date.atTime(LocalTime.MAX);
        var existingAppointments = appointmentRepository.findByDoctorIdAndStartTimeBetweenAndStatusNot(
                doctorId, startOfDay, endOfDay, AppointmentStatus.CANCELLED);
        var isToday = date.equals(now.toLocalDate());
        var availableSlots = createGeneratedSlots(availabilities).stream()
                .distinct()
                .filter(slot -> !isToday || slot.startTime().isAfter(now.toLocalTime()))
                .filter(slot -> exceptions.stream().noneMatch(e -> e.blocks(slot.startTime(), slot.endTime())))
                .filter(slot -> existingAppointments.stream()
                        .noneMatch(a -> slot.startTime().isBefore(a.getEndTime().toLocalTime())
                                && slot.endTime().isAfter(a.getStartTime().toLocalTime())))
                .sorted(Comparator.comparing(SlotResponse::startTime))
                .toList();
        return new DoctorAvailabilitySlotsResponse(date, doctorId, availableSlots);
    }

    @Override
    @Transactional
    public void deactivateDoctorAvailabilityById(Long availabilityId, Long doctorId) {
        var availability = doctorAvailabilityRepository
                .findByIdAndActiveIsTrue(availabilityId)
                .orElseThrow(() -> new ResourceNotFoundException(MessageKeys.AVAILABILITY_NOT_FOUND, availabilityId));
        if (!availability.getDoctor().getId().equals(doctorId))
            throw new ForbiddenException(MessageKeys.AVAILABILITY_UNAUTHORIZED);
        availability.setActive(false);
    }

    private List<SlotResponse> createGeneratedSlots(List<DoctorAvailability> availabilities) {
        var generatedSlots = new ArrayList<SlotResponse>();
        for (var availability : availabilities) {
            var duration = availability.getSlotDurationMinutes();
            if (duration == null || duration <= 0) continue;
            var end = availability.getEndTime().toSecondOfDay() / 60;
            for (int m = availability.getStartTime().toSecondOfDay() / 60; m + duration <= end; m += duration)
                generatedSlots.add(new SlotResponse(
                        LocalTime.ofSecondOfDay(m * 60L), LocalTime.ofSecondOfDay((m + duration) * 60L)));
        }
        return generatedSlots;
    }
}
