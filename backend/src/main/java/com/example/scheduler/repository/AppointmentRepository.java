package com.example.scheduler.repository;

import com.example.scheduler.entity.Appointment;
import com.example.scheduler.enums.AppointmentStatus;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.jspecify.annotations.NullMarked;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AppointmentRepository extends JpaRepository<Appointment, Long> {
    String SELECT_APPOINTMENT = "SELECT a FROM Appointment a ";
    String FETCH_DOCTOR = "JOIN FETCH a.doctor d ";
    String FETCH_PATIENT = "JOIN FETCH a.patient p ";
    String DOCTOR_AND_PATIENT_FILTER = "WHERE (:doctorId IS NULL OR a.doctor.id = :doctorId) AND "
            + "(:patientId IS NULL OR a.patient.id = :patientId) AND ";
    String OVERLAPPING_CONDITION = "WHERE a.doctor.id = :doctorId " + "AND a.status NOT IN :excludedStatuses "
            + "AND a.startTime < :endTime "
            + "AND a.endTime > :startTime";
    String GRAPH_DOCTOR_SPECIALTY = "doctor.specialty";
    String GRAPH_PATIENT = "patient";

    @Query(SELECT_APPOINTMENT + FETCH_DOCTOR
            + "LEFT JOIN FETCH d.specialty sp "
            + FETCH_PATIENT
            + DOCTOR_AND_PATIENT_FILTER
            + "(:status IS NULL OR a.status = :status)")
    Page<Appointment> findAllByFilters(
            @Param("doctorId") Long doctorId,
            @Param("patientId") Long patientId,
            @Param("status") AppointmentStatus status,
            Pageable pageable);

    @Query("SELECT a FROM Appointment a  " + FETCH_DOCTOR
            + FETCH_PATIENT
            + DOCTOR_AND_PATIENT_FILTER
            + "a.startTime >= :from AND a.startTime < :to "
            + "ORDER BY a.startTime ASC")
    List<Appointment> findByFiltersAndDateRange(
            @Param("doctorId") Long doctorId,
            @Param("patientId") Long patientId,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to);

    @Query("SELECT COUNT(a) > 0 FROM Appointment a " + OVERLAPPING_CONDITION)
    boolean existsOverlappingAppointment(
            @Param("doctorId") Long doctorId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime,
            @Param("excludedStatuses") List<AppointmentStatus> excludedStatuses);

    @EntityGraph(attributePaths = {GRAPH_DOCTOR_SPECIALTY, GRAPH_PATIENT})
    @Query(SELECT_APPOINTMENT + OVERLAPPING_CONDITION)
    List<Appointment> findOverlappingAppointments(
            @Param("doctorId") Long doctorId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime,
            @Param("excludedStatuses") List<AppointmentStatus> excludedStatuses);

    boolean existsByDoctorIdAndStatusNotAndStartTimeAfter(Long doctorId, AppointmentStatus status, LocalDateTime time);

    List<Appointment> findByDoctorIdAndStartTimeBetweenAndStatusNot(
            Long doctorId, LocalDateTime start, LocalDateTime end, AppointmentStatus status);

    @NullMarked
    @EntityGraph(attributePaths = {GRAPH_DOCTOR_SPECIALTY, GRAPH_PATIENT})
    Optional<Appointment> findById(Long id);
}
