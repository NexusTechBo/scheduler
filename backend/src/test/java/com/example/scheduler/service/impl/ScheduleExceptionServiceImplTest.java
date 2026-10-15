package com.example.scheduler.service.impl;

import com.example.scheduler.dto.ScheduleException.ScheduleExceptionRequest;
import com.example.scheduler.entity.Personal;
import com.example.scheduler.entity.ScheduleException;
import com.example.scheduler.exception.BadRequestException;
import com.example.scheduler.mapper.ScheduleExceptionMapper;
import com.example.scheduler.repository.PersonalRepository;
import com.example.scheduler.repository.ScheduleExceptionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ScheduleExceptionServiceImpl: B11 validation")
class ScheduleExceptionServiceImplTest {
    private static final long DOCTOR_ID = 7L;
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-10-15T02:00:00Z"), ZoneId.of("America/La_Paz"));
    private static final LocalDate TODAY = LocalDate.of(2026, 10, 14);

    @Mock ScheduleExceptionRepository scheduleExceptionRepository;
    @Mock PersonalRepository personalRepository;
    @Mock ScheduleExceptionMapper mapper;
    ScheduleExceptionServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new ScheduleExceptionServiceImpl(scheduleExceptionRepository, personalRepository, mapper, CLOCK);
    }

    @Test
    @DisplayName("date before today -> 400 in the past")
    void add_should_reject_when_dateInThePast() {
        assertRejected(new ScheduleExceptionRequest(TODAY.minusDays(1), null, null, true, null),
                "The block date cannot be in the past");
    }

    @ParameterizedTest(name = "start={0} end={1} -> 400")
    @CsvSource({",", "09:00,", ",12:00"})
    @DisplayName("partial block without start or end time -> 400")
    void add_should_reject_when_partialBlockMissingTimes(LocalTime start, LocalTime end) {
        assertRejected(new ScheduleExceptionRequest(TODAY, start, end, false, null),
                "A partial block needs start and end time");
    }

    @ParameterizedTest(name = "start={0} end={1} -> 400")
    @CsvSource({"12:00,09:00", "09:00,09:00"})
    @DisplayName("partial block with end not after start -> 400")
    void add_should_reject_when_partialBlockRangeInverted(LocalTime start, LocalTime end) {
        assertRejected(new ScheduleExceptionRequest(TODAY, start, end, false, null),
                "The end time must be after the start time");
    }

    @Test
    @DisplayName("full-day block today without times -> saved")
    void add_should_save_when_fullDayBlockToday() {
        stubSave();

        service.addDoctorScheduleException(DOCTOR_ID, new ScheduleExceptionRequest(TODAY, null, null, true, null));

        verify(scheduleExceptionRepository).save(any());
    }

    @Test
    @DisplayName("valid partial block -> saved")
    void add_should_save_when_partialBlockValid() {
        stubSave();

        service.addDoctorScheduleException(DOCTOR_ID,
                new ScheduleExceptionRequest(TODAY.plusDays(1), LocalTime.of(9, 0), LocalTime.of(12, 0), false, "Congreso"));

        verify(scheduleExceptionRepository).save(any());
    }

    @Test
    @DisplayName("list without ?from defaults to today in the clock's zone")
    void find_should_defaultFromToClockToday() {
        when(scheduleExceptionRepository.findByDoctorIdAndDateGreaterThanEqualOrderByDateAscStartTimeAsc(DOCTOR_ID, TODAY))
                .thenReturn(List.of());

        service.findDoctorScheduleExceptions(DOCTOR_ID, null);

        verify(scheduleExceptionRepository).findByDoctorIdAndDateGreaterThanEqualOrderByDateAscStartTimeAsc(DOCTOR_ID, TODAY);
    }

    private void assertRejected(ScheduleExceptionRequest request, String message) {
        assertThatThrownBy(() -> service.addDoctorScheduleException(DOCTOR_ID, request))
                .isInstanceOf(BadRequestException.class)
                .hasMessage(message);
        verifyNoInteractions(scheduleExceptionRepository);
    }

    private void stubSave() {
        when(personalRepository.findById(DOCTOR_ID)).thenReturn(Optional.of(new Personal()));
        when(mapper.toEntity(any())).thenReturn(new ScheduleException());
        when(scheduleExceptionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }
}
