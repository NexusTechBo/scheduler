package com.example.scheduler.repository;

import com.example.scheduler.entity.ScheduleException;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface ScheduleExceptionRepository extends JpaRepository<ScheduleException, Long> {
    @EntityGraph(attributePaths = {"doctor.specialty"})
    List<ScheduleException> findByDoctorIdAndDate(Long doctorId, LocalDate date);
    @EntityGraph(attributePaths = {"doctor.specialty"})
    List<ScheduleException> findByDoctorIdAndDateGreaterThanEqualOrderByDateAscStartTimeAsc(Long doctorId, LocalDate from);
}
