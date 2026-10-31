package com.example.scheduler.repository;

import com.example.scheduler.entity.Patient;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PatientRepository extends JpaRepository<Patient, Long> {
    @Query("SELECT p FROM Patient p WHERE (:isActive IS NULL OR p.active = :isActive) AND ("
            + "LOWER(p.name) LIKE LOWER(CONCAT('%', :search, '%')) OR "
            + "p.ci LIKE CONCAT('%', :search, '%') OR "
            + "LOWER(p.email) LIKE LOWER(CONCAT('%', :search, '%')) OR "
            + "p.phoneNumber LIKE CONCAT('%', :search, '%'))")
    Page<Patient> search(@Param("search") String search, @Param("isActive") Boolean isActive, Pageable pageable);

    @EntityGraph(attributePaths = {"doctors.specialty"})
    Optional<Patient> findPatientDoctorsById(Long id);

    Optional<Patient> findByPhoneNumber(String phoneNumber);

    boolean existsByCi(String ci);

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByPhoneNumber(String phoneNumber);

    @Query("SELECT COUNT(p) > 0 FROM Patient p "
            + "WHERE p.ci = :ci OR p.phoneNumber = :phoneNumber OR LOWER(p.email) = LOWER(:email)")
    boolean existsByCiOrPhoneNumberOrEmailIgnoreCase(
            @Param("ci") String ci, @Param("phoneNumber") String phoneNumber, @Param("email") String email);

    Optional<Patient> findByCiAndPhoneNumberAndActiveIsTrue(String ci, String phoneNumber);
}
