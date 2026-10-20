package com.example.scheduler.repository;

import com.example.scheduler.entity.DoctorAvailability;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.DayOfWeek;
import java.util.List;
import java.util.Optional;

public interface DoctorAvailabilityRepository extends JpaRepository<DoctorAvailability, Long> {
    @EntityGraph(attributePaths = {"doctor.specialty"})
    List<DoctorAvailability> findByDoctorIdAndActiveTrue(Long doctorId);
    @EntityGraph(attributePaths = {"doctor.specialty"})
    List<DoctorAvailability> findByDoctorIdAndDayOfWeekAndActiveTrue(Long doctorId, DayOfWeek dayOfWeek);
    @EntityGraph(attributePaths = {"doctor.specialty"})
    Optional<DoctorAvailability> findByIdAndActiveIsTrue(Long id);
}
