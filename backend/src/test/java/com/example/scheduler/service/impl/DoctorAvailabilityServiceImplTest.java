package com.example.scheduler.service.impl;

import com.example.scheduler.dto.DoctorAvailability.DoctorAvailabilityRequest;
import com.example.scheduler.dto.DoctorAvailability.DoctorAvailabilitySlotsResponse.Slot;
import com.example.scheduler.entity.Appointment;
import com.example.scheduler.entity.DoctorAvailability;
import com.example.scheduler.entity.Personal;
import com.example.scheduler.entity.Role;
import com.example.scheduler.entity.ScheduleException;
import com.example.scheduler.enums.ERole;
import com.example.scheduler.exception.BusinessException;
import com.example.scheduler.mapper.DoctorAvailabilityMapper;
import com.example.scheduler.repository.AppointmentRepository;
import com.example.scheduler.repository.DoctorAvailabilityRepository;
import com.example.scheduler.repository.PersonalRepository;
import com.example.scheduler.repository.ScheduleExceptionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DoctorAvailabilityServiceImplTest {
    private static final long DOCTOR_ID = 7L;
    private static final LocalDate DATE = LocalDate.of(2099, 10, 12);
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-10-15T14:00:00Z"), ZoneId.of("America/La_Paz"));

    @Mock DoctorAvailabilityRepository availabilityRepository;
    @Mock ScheduleExceptionRepository scheduleExceptionRepository;
    @Mock AppointmentRepository appointmentRepository;
    @Mock PersonalRepository personalRepository;
    @Mock DoctorAvailabilityMapper mapper;
    DoctorAvailabilityServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new DoctorAvailabilityServiceImpl(availabilityRepository, scheduleExceptionRepository, appointmentRepository,
                personalRepository, mapper, CLOCK);
        lenient().when(personalRepository.findById(DOCTOR_ID)).thenReturn(Optional.of(personal(ERole.DOCTOR, true)));
    }

    @ParameterizedTest(name = "role={0}, active={1} -> no slots")
    @CsvSource({"ASSISTANT,true", "DOCTOR,false"})
    @DisplayName("B20: path id that is not an active doctor -> empty slot list, no blocks queried")
    void slots_should_beEmpty_when_notActiveDoctor(ERole role, boolean active) {
        when(personalRepository.findById(DOCTOR_ID)).thenReturn(Optional.of(personal(role, active)));

        assertThat(startTimes()).isEmpty();
        verifyNoInteractions(availabilityRepository);
    }

    @Test
    @DisplayName("B20: unknown path id -> empty slot list")
    void slots_should_beEmpty_when_doctorUnknown() {
        when(personalRepository.findById(DOCTOR_ID)).thenReturn(Optional.empty());

        assertThat(startTimes()).isEmpty();
        verifyNoInteractions(availabilityRepository);
    }

    private static Personal personal(ERole role, boolean active) {
        return Personal.builder().id(DOCTOR_ID).active(active).role(Role.builder().name(role).build()).build();
    }

    @Test
    @DisplayName("B10: new block overlapping an active block of the same day -> 409, nothing saved")
    void add_should_reject_when_overlapsExistingBlock() {
        when(availabilityRepository.findByDoctorIdAndDayOfWeekAndActiveTrue(DOCTOR_ID, DayOfWeek.MONDAY))
                .thenReturn(List.of(block(8, 0, 12, 0, 30)));

        assertThatThrownBy(() -> service.addDoctorAvailability(DOCTOR_ID, monday(9, 0, 12, 0)))
                .isInstanceOf(BusinessException.class)
                .hasMessage("That availability overlaps an existing block");
        verify(availabilityRepository, never()).save(any());
    }

    @Test
    @DisplayName("B10: adjacent block 12:00-14:00 after 08:00-12:00 -> saved")
    void add_should_save_when_adjacentBlock() {
        when(availabilityRepository.findByDoctorIdAndDayOfWeekAndActiveTrue(DOCTOR_ID, DayOfWeek.MONDAY))
                .thenReturn(List.of(block(8, 0, 12, 0, 30)));
        when(personalRepository.findById(DOCTOR_ID)).thenReturn(Optional.of(new Personal()));

        service.addDoctorAvailability(DOCTOR_ID, monday(12, 0, 14, 0));

        verify(availabilityRepository).save(any());
    }

    @Test
    @DisplayName("B9: past date -> empty slot list without querying blocks")
    void slots_should_beEmpty_when_dateInPast() {
        var response = service.getDoctorAvailableSlots(DOCTOR_ID, LocalDate.now(CLOCK).minusDays(1));

        assertThat(response.availableSlots()).isEmpty();
        verifyNoInteractions(availabilityRepository);
    }

    @Test
    @DisplayName("B9: today -> only slots starting after now")
    void slots_should_onlyBeFuture_when_today() {
        var today = LocalDate.of(2026, 10, 15);
        when(availabilityRepository.findByDoctorIdAndDayOfWeekAndActiveTrue(DOCTOR_ID, today.getDayOfWeek()))
                .thenReturn(List.of(block(0, 0, 23, 59, 15)));
        var before = LocalTime.of(10, 0);

        var starts = service.getDoctorAvailableSlots(DOCTOR_ID, today).availableSlots().stream().map(Slot::startTime).toList();

        assertThat(starts).allMatch(t -> t.isAfter(before), "every slot starts after now");
        assertThat(starts.getFirst()).isEqualTo(LocalTime.of(10, 15));
    }

    @Test
    @DisplayName("B8: partial block 08:45-09:15 hides every slot overlapping it, not only those starting inside")
    void slots_should_hideSlotsOverlappingPartialBlock() {
        stubBlocks(block(8, 0, 10, 0, 30));
        when(scheduleExceptionRepository.findByDoctorIdAndDate(DOCTOR_ID, DATE))
                .thenReturn(List.of(ScheduleException.builder().isFullDayBlock(false)
                        .startTime(LocalTime.of(8, 45)).endTime(LocalTime.of(9, 15)).build()));

        assertThat(startTimes()).containsExactly(LocalTime.of(8, 0), LocalTime.of(9, 30));
    }

    private static DoctorAvailabilityRequest monday(int sh, int sm, int eh, int em) {
        return new DoctorAvailabilityRequest(DayOfWeek.MONDAY, LocalTime.of(sh, sm), LocalTime.of(eh, em), 30);
    }

    @Test
    @Timeout(2)
    void slots_should_terminate_when_blockEndsNearMidnightOrDurationIsZero() {
        stubBlocks(block(23, 0, 23, 59, 30), block(9, 0, 12, 0, 0));

        assertThat(startTimes()).containsExactly(LocalTime.of(23, 0));
    }

    @Test
    void slots_should_dedupeAndHideSlotsOverlappingAnAppointment() {
        stubBlocks(block(9, 0, 11, 0, 30), block(9, 0, 11, 0, 30), block(10, 0, 11, 0, 20));
        when(appointmentRepository.findByDoctorIdAndStartTimeBetweenAndStatusNot(eq(DOCTOR_ID), any(), any(), any()))
                .thenReturn(List.of(Appointment.builder().startTime(DATE.atTime(10, 0)).endTime(DATE.atTime(10, 30)).build()));

        assertThat(startTimes()).containsExactly(LocalTime.of(9, 0), LocalTime.of(9, 30), LocalTime.of(10, 30), LocalTime.of(10, 40));
    }

    private List<LocalTime> startTimes() {
        return service.getDoctorAvailableSlots(DOCTOR_ID, DATE).availableSlots().stream().map(Slot::startTime).toList();
    }

    private void stubBlocks(DoctorAvailability... blocks) {
        when(availabilityRepository.findByDoctorIdAndDayOfWeekAndActiveTrue(DOCTOR_ID, DATE.getDayOfWeek())).thenReturn(List.of(blocks));
    }

    private static DoctorAvailability block(int sh, int sm, int eh, int em, int duration) {
        return DoctorAvailability.builder().startTime(LocalTime.of(sh, sm)).endTime(LocalTime.of(eh, em)).slotDurationMinutes(duration).build();
    }
}
