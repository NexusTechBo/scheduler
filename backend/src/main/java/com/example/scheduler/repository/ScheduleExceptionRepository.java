package com.example.scheduler.repository;

import com.example.scheduler.entity.ScheduleException;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ScheduleExceptionRepository extends JpaRepository<ScheduleException, Long> {
    String GRAPH_DOCTOR_SPECIALTY = "doctor.specialty";

    @EntityGraph(attributePaths = {GRAPH_DOCTOR_SPECIALTY})
    List<ScheduleException> findByDoctorIdAndDate(Long doctorId, LocalDate date);

    @EntityGraph(attributePaths = {GRAPH_DOCTOR_SPECIALTY})
    List<ScheduleException> findByDoctorIdAndDateGreaterThanEqualOrderByDateAscStartTimeAsc(
            Long doctorId, LocalDate from);
}
