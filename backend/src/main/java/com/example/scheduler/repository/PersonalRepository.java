package com.example.scheduler.repository;

import com.example.scheduler.entity.Personal;
import com.example.scheduler.enums.ERole;
import jakarta.persistence.LockModeType;
import org.jspecify.annotations.NullMarked;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PersonalRepository extends JpaRepository<Personal, Long> {
    @Query("SELECT p FROM Personal p " +
            "LEFT JOIN FETCH p.specialty s " +
            "WHERE p.role = ERole.DOCTOR AND " +
            "(:specialtyId IS NULL OR s.id = :specialtyId) AND " +
            "(:isActive IS NULL OR p.active = :isActive)")
    Page<Personal> findAllDoctorsByFilters(@Param("specialtyId") Long specialtyId, @Param("isActive") Boolean isActive, Pageable pageable);

    @Query("SELECT p FROM Personal p " +
            "LEFT JOIN FETCH p.specialty s " +
            "WHERE p.role = ERole.DOCTOR AND " +
            "p.active = true AND " +
            "(:specialtyId IS NULL OR s.id = :specialtyId)")
    List<Personal> findAllDoctorsActive(@Param("specialtyId") Long specialtyId);

    @Query("SELECT p FROM Personal p " +
            "LEFT JOIN FETCH p.specialty s " +
            "WHERE (:role IS NULL OR p.role = :role) AND " +
            "(:specialtyId IS NULL OR p.specialty.id = :specialtyId) AND " +
            "(:isActive IS NULL OR p.active = :isActive)")
    Page<Personal> findAllByFilters(@Param("specialtyId") Long specialtyId, @Param("isActive") Boolean isActive, @Param("role") ERole role, Pageable pageable);

    Optional<Personal> findByUsername(String username);

    Optional<Personal> findFirstByRoleOrderByIdAsc(ERole role);

    @NullMarked
    @EntityGraph(attributePaths = {"specialty"})
    Optional<Personal> findById(Long id);


    Optional<Personal> findByIdAndRoleAndActiveIsTrue(Long id, ERole role);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Personal p WHERE p.id = :id")
    Optional<Personal> findByIdForUpdate(@Param("id") Long id);

    @EntityGraph(attributePaths = {"patients"})
    Optional<Personal> findDoctorPatientsByIdAndRole(Long id, ERole role);

    boolean existsByUsername(String username);

    boolean existsByCi(String ci);

    boolean existsByEmail(String email);

    boolean existsByPhoneNumber(String phoneNumber);

    boolean existsByIdAndActiveTrueAndRole(Long id, ERole role);
}
