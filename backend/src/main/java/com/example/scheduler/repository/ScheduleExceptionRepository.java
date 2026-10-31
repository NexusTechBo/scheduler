package com.example.scheduler.repository;

import com.example.scheduler.entity.ScheduleException;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ScheduleExceptionRepository extends JpaRepository<ScheduleException, Long> {
    @EntityGraph(attributePaths = {"doctor.specialty"})
    List<ScheduleException> findByDoctorIdAndDate(Long doctorId, LocalDate date);

    @EntityGraph(attributePaths = {"doctor.specialty"})
    List<ScheduleException> findByDoctorIdAndDateGreaterThanEqualOrderByDateAscStartTimeAsc(
            Long doctorId, LocalDate from);
}
