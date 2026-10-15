package com.example.scheduler.repository;

import com.example.scheduler.entity.Patient;
import org.jspecify.annotations.NullMarked;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface PatientRepository extends JpaRepository<Patient, Long> {
    @Query("SELECT p FROM Patient p " +
            "JOIN FETCH p.account a " +
            "JOIN FETCH p.role r " +
            "WHERE a.email = :email")
    Optional<Patient> findByAccountEmail(@Param("email") String email);

    @Query("SELECT p FROM Patient p " +
            "JOIN FETCH p.account a " +
            "JOIN FETCH p.role r " +
            "WHERE a.phoneNumber = :phoneNumber")
    Optional<Patient> findByAccountPhoneNumber(@Param("phoneNumber") String phoneNumber);

    @NullMarked
    @EntityGraph(attributePaths = {"account"})
    Page<Patient> findAll(Pageable pageable);

    @NullMarked
    @EntityGraph(attributePaths = {"account"})
    Optional<Patient> findById(Long id);

    @EntityGraph(attributePaths = {"doctors.account", "doctors.specialty", "doctors.role"})
    Optional<Patient> findPatientDoctorsById(Long id);

    boolean existsByAccountCi(String ci);
}
