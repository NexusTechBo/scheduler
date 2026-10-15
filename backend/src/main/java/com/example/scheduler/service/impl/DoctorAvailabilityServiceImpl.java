package com.example.scheduler.service.impl;

import com.example.scheduler.dto.DoctorAvailability.DoctorAvailabilitySlotsResponse;
import com.example.scheduler.dto.DoctorAvailability.DoctorAvailabilitySlotsResponse.Slot;
import com.example.scheduler.dto.DoctorAvailability.DoctorAvailabilityRequest;
import com.example.scheduler.dto.DoctorAvailability.DoctorAvailabilityResponse;
import com.example.scheduler.entity.DoctorAvailability;
import com.example.scheduler.enums.AppointmentStatus;
import com.example.scheduler.exception.BadRequestException;
import com.example.scheduler.exception.ForbiddenException;
import com.example.scheduler.exception.ResourceNotFoundException;
import com.example.scheduler.mapper.DoctorAvailabilityMapper;
import com.example.scheduler.repository.AppointmentRepository;
import com.example.scheduler.repository.DoctorAvailabilityRepository;
import com.example.scheduler.repository.PersonalRepository;
import com.example.scheduler.repository.ScheduleExceptionRepository;
import com.example.scheduler.service.DoctorAvailabilityService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DoctorAvailabilityServiceImpl implements DoctorAvailabilityService {
    private final DoctorAvailabilityRepository availabilityRepository;
    private final ScheduleExceptionRepository scheduleExceptionRepository;
    private final AppointmentRepository appointmentRepository;
    private final PersonalRepository personalRepository;
    private final DoctorAvailabilityMapper mapper;

    @Override
    @Transactional
    public DoctorAvailabilityResponse addDoctorAvailability(Long doctorId, DoctorAvailabilityRequest request) {
        if (request.startTime().isAfter(request.endTime()) || request.startTime().equals(request.endTime()))
            throw new BadRequestException("the start time must be after the end time");
        var doctor = personalRepository.findById(doctorId)
                .orElseThrow(() -> new ResourceNotFoundException("Doctor not found with id: " + doctorId));
        var availability = DoctorAvailability.builder()
                .doctor(doctor)
                .dayOfWeek(request.dayOfWeek())
                .startTime(request.startTime())
                .endTime(request.endTime())
                .slotDurationMinutes(request.slotDurationMinutes())
                .active(true)
                .build();
        return mapper.toResponse(availabilityRepository.save(availability));
    }

    @Override
    public List<DoctorAvailabilityResponse> getDoctorAvailabilities(Long doctorId) {
        return mapper.toResponseList(availabilityRepository.findByDoctorIdAndActiveTrue(doctorId));
    }

    @Override
    public DoctorAvailabilitySlotsResponse getDoctorAvailableSlots(Long doctorId, LocalDate date) {
        var isFullDayBlocked = scheduleExceptionRepository.existsByDoctorIdAndDateAndIsFullDayBlockTrue(doctorId, date);
        if (isFullDayBlocked)
            return new DoctorAvailabilitySlotsResponse(date, doctorId, List.of());
        var dayOfWeek = date.getDayOfWeek();
        var availabilities = availabilityRepository
                .findByDoctorIdAndDayOfWeekAndActiveTrue(doctorId, dayOfWeek);
        if (availabilities.isEmpty())
            return new DoctorAvailabilitySlotsResponse(date, doctorId, List.of());
        var generatedSlots = createGeneratedSlots(availabilities);
        var partialExceptions = scheduleExceptionRepository.findByDoctorIdAndDate(doctorId, date)
                .stream()
                .filter(e -> !e.getIsFullDayBlock() && e.getStartTime() != null && e.getEndTime() != null)
                .toList();
        var slotsAfterExceptions = generatedSlots.stream()
                .filter(slot -> partialExceptions.stream()
                        .noneMatch(e ->
                                !slot.startTime().isBefore(e.getStartTime()) && slot.startTime().isBefore(e.getEndTime()))
                )
                .toList();
        var startOfDay = date.atStartOfDay();
        var endOfDay = date.atTime(LocalTime.MAX);
        var existingAppointments = appointmentRepository.findByDoctorIdAndStartTimeBetweenAndStatusNot(
                doctorId,
                startOfDay,
                endOfDay,
                AppointmentStatus.CANCELLED
        );
        var bookedStartTimes = existingAppointments.stream()
                .map(a -> a.getStartTime().toLocalTime())
                .toList();
        var availableSlots = slotsAfterExceptions.stream()
                .filter(slot -> !bookedStartTimes.contains(slot.startTime()))
                .sorted(Comparator.comparing(Slot::startTime))
                .toList();
        return new DoctorAvailabilitySlotsResponse(date, doctorId, availableSlots);
    }

    @Override
    @Transactional
    public void deactivateDoctorAvailabilityById(Long availabilityId, Long doctorId) {
        var availability = availabilityRepository.findById(availabilityId)
                .filter(DoctorAvailability::getActive)
                .orElseThrow(() -> new ResourceNotFoundException("Availability not found with id: " + availabilityId));
        if (!availability.getDoctor().getId().equals(doctorId))
            throw new ForbiddenException("Not authorize to do this");
        availability.setActive(false);
    }

    private List<Slot> createGeneratedSlots(List<DoctorAvailability> availabilities) {
        var generatedSlots = new ArrayList<Slot>();
        for (var availability : availabilities) {
            var current = availability.getStartTime();
            var end = availability.getEndTime();
            var duration = availability.getSlotDurationMinutes();
            while (current.plusMinutes(duration).isBefore(end) || current.plusMinutes(duration).equals(end)) {
                var next = current.plusMinutes(duration);
                generatedSlots.add(new Slot(current, next));
                current = next;
            }
        }
        return generatedSlots;
    }
}
