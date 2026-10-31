package com.example.scheduler.repository;

import com.example.scheduler.entity.Personal;
import com.example.scheduler.enums.ERole;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.jspecify.annotations.NullMarked;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PersonalRepository extends JpaRepository<Personal, Long> {
    String SELECT_PERSONAL = "SELECT p FROM Personal p ";
    String FETCH_SPECIALTY = "LEFT JOIN FETCH p.specialty s ";
    String WHERE_DOCTOR = "WHERE p.role = ERole.DOCTOR AND ";
    String SPECIALTY_FILTER = "(:specialtyId IS NULL OR s.id = :specialtyId)";
    String ACTIVE_FILTER = "(:isActive IS NULL OR p.active = :isActive)";

    @Query(SELECT_PERSONAL + FETCH_SPECIALTY + WHERE_DOCTOR + SPECIALTY_FILTER + " AND " + ACTIVE_FILTER)
    Page<Personal> findAllDoctorsByFilters(
            @Param("specialtyId") Long specialtyId, @Param("isActive") Boolean isActive, Pageable pageable);

    @Query(SELECT_PERSONAL + FETCH_SPECIALTY + WHERE_DOCTOR + "p.active = true AND " + SPECIALTY_FILTER)
    List<Personal> findAllDoctorsActive(@Param("specialtyId") Long specialtyId);

    @Query(SELECT_PERSONAL + FETCH_SPECIALTY
            + "WHERE (:role IS NULL OR p.role = :role) AND "
            + "(:specialtyId IS NULL OR p.specialty.id = :specialtyId) AND "
            + ACTIVE_FILTER)
    Page<Personal> findAllByFilters(
            @Param("specialtyId") Long specialtyId,
            @Param("isActive") Boolean isActive,
            @Param("role") ERole role,
            Pageable pageable);

    Optional<Personal> findByUsername(String username);

    Optional<Personal> findFirstByRoleOrderByIdAsc(ERole role);

    @NullMarked
    @EntityGraph(attributePaths = {"specialty"})
    Optional<Personal> findById(Long id);

    Optional<Personal> findByIdAndRoleAndActiveIsTrue(Long id, ERole role);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query(SELECT_PERSONAL + "WHERE p.id = :id")
    Optional<Personal> findByIdForUpdate(@Param("id") Long id);

    @EntityGraph(attributePaths = {"patients"})
    Optional<Personal> findDoctorPatientsByIdAndRole(Long id, ERole role);

    boolean existsByUsername(String username);

    boolean existsByCi(String ci);

    boolean existsByEmail(String email);

    boolean existsByPhoneNumber(String phoneNumber);

    boolean existsByIdAndActiveTrueAndRole(Long id, ERole role);
}
