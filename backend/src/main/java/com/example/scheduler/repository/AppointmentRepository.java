package com.example.scheduler.repository;

import com.example.scheduler.entity.Appointment;
import com.example.scheduler.enums.AppointmentStatus;
import java.time.LocalDateTime;
import java.util.Collection;
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
    @Query("SELECT a FROM Appointment a " + "JOIN FETCH a.doctor d "
            + "LEFT JOIN FETCH d.specialty sp "
            + "JOIN FETCH a.patient p "
            + "WHERE (:doctorId IS NULL OR a.doctor.id = :doctorId) AND "
            + "(:patientId IS NULL OR a.patient.id = :patientId) AND "
            + "(:status IS NULL OR a.status = :status)")
    Page<Appointment> findAllByFilters(
            @Param("doctorId") Long doctorId,
            @Param("patientId") Long patientId,
            @Param("status") AppointmentStatus status,
            Pageable pageable);

    @Query("SELECT a FROM Appointment a  " + "JOIN FETCH a.doctor d "
            + "JOIN FETCH a.patient p "
            + "WHERE (:doctorId IS NULL OR a.doctor.id = :doctorId) AND "
            + "(:patientId IS NULL OR a.patient.id = :patientId) AND "
            + "a.startTime >= :from AND a.startTime < :to "
            + "ORDER BY a.startTime ASC")
    List<Appointment> findByFiltersAndDateRange(
            @Param("doctorId") Long doctorId,
            @Param("patientId") Long patientId,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to);

    /** {@code excludeId} skips one appointment (the one being rescheduled); pass null to check every appointment. */
    @Query("SELECT COUNT(a) > 0 FROM Appointment a " + "WHERE a.doctor.id = :doctorId "
            + "AND a.status NOT IN :excludedStatuses "
            + "AND a.startTime < :endTime "
            + "AND a.endTime > :startTime "
            + "AND (:excludeId IS NULL OR a.id <> :excludeId)")
    boolean existsOverlappingAppointment(
            @Param("doctorId") Long doctorId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime,
            @Param("excludedStatuses") List<AppointmentStatus> excludedStatuses,
            @Param("excludeId") Long excludeId);

    @EntityGraph(attributePaths = {"doctor.specialty", "patient"})
    @Query("SELECT a FROM Appointment a " + "WHERE a.doctor.id = :doctorId "
            + "AND a.status NOT IN :excludedStatuses "
            + "AND a.startTime < :endTime "
            + "AND a.endTime > :startTime "
            + "ORDER BY a.id")
    List<Appointment> findOverlappingAppointments(
            @Param("doctorId") Long doctorId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime,
            @Param("excludedStatuses") List<AppointmentStatus> excludedStatuses);

    @Query("SELECT COUNT(a) FROM Appointment a " + "WHERE a.doctor.id = :doctorId "
            + "AND a.status NOT IN :excludedStatuses "
            + "AND a.startTime < :endTime "
            + "AND a.endTime > :startTime")
    long countOverlappingAppointments(
            @Param("doctorId") Long doctorId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime,
            @Param("excludedStatuses") List<AppointmentStatus> excludedStatuses);

    @EntityGraph(attributePaths = {"doctor.specialty", "patient"})
    List<Appointment> findByDoctorIdAndStatusInAndStartTimeAfterOrderByIdAsc(
            Long doctorId, Collection<AppointmentStatus> statuses, LocalDateTime time);

    @EntityGraph(attributePaths = {"doctor.specialty", "patient"})
    List<Appointment> findByPatientIdAndStatusInAndStartTimeAfterOrderByIdAsc(
            Long patientId, Collection<AppointmentStatus> statuses, LocalDateTime time);

    @Query("SELECT a.doctor.id FROM Appointment a WHERE a.id = :id")
    Optional<Long> findDoctorIdById(@Param("id") Long id);

    Optional<Appointment> findFirstByPatientIdAndDoctorIdAndStatusInAndStartTimeAfterOrderByStartTimeAsc(
            Long patientId, Long doctorId, Collection<AppointmentStatus> statuses, LocalDateTime time);

    boolean existsByDoctorIdAndStatusNotAndStartTimeAfter(Long doctorId, AppointmentStatus status, LocalDateTime time);

    List<Appointment> findByDoctorIdAndStartTimeBetweenAndStatusNot(
            Long doctorId, LocalDateTime start, LocalDateTime end, AppointmentStatus status);

    @NullMarked
    @EntityGraph(attributePaths = {"doctor.specialty", "patient"})
    Optional<Appointment> findById(Long id);
}
