package com.example.scheduler.service.impl;

import com.example.scheduler.dto.ScheduleException.ScheduleExceptionRequest;
import com.example.scheduler.dto.ScheduleException.ScheduleExceptionResponse;
import com.example.scheduler.exception.ForbiddenException;
import com.example.scheduler.exception.ResourceNotFoundException;
import com.example.scheduler.mapper.ScheduleExceptionMapper;
import com.example.scheduler.repository.PersonalRepository;
import com.example.scheduler.repository.ScheduleExceptionRepository;
import com.example.scheduler.service.ScheduleExceptionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ScheduleExceptionServiceImpl implements ScheduleExceptionService {
    private final ScheduleExceptionRepository scheduleExceptionRepository;
    private final PersonalRepository doctorRepository;
    private final ScheduleExceptionMapper mapper;

    @Override
    @Transactional
    public ScheduleExceptionResponse addDoctorScheduleException(Long doctorId, ScheduleExceptionRequest request) {
        var doctor = doctorRepository.findById(doctorId)
                .orElseThrow(() -> new ResourceNotFoundException("Doctor not found with id: " + doctorId));
        var scheduleException = mapper.toEntity(request);
        scheduleException.setDoctor(doctor);
        return mapper.toResponse(scheduleExceptionRepository.save(scheduleException));
    }

    @Override
    public List<ScheduleExceptionResponse> findDoctorScheduleExceptions(Long doctorId, LocalDate from) {
        var since = from != null ? from : LocalDate.now();
        return scheduleExceptionRepository.findByDoctorIdAndDateGreaterThanEqualOrderByDateAscStartTimeAsc(doctorId, since)
                .stream()
                .map(mapper::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public void deleteDoctorScheduleExceptionById(Long scheduleExceptionId, Long doctorId) {
        var scheduleException = scheduleExceptionRepository.findById(scheduleExceptionId)
                .orElseThrow(() -> new ResourceNotFoundException("Schedule exception not found with id: " + scheduleExceptionId));
        if (!scheduleException.getDoctor().getId().equals(doctorId))
            throw new ForbiddenException("Not authorize to do this");
        scheduleExceptionRepository.delete(scheduleException);
    }
}
