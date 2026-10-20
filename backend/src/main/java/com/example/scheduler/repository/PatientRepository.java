package com.example.scheduler.repository;

import com.example.scheduler.entity.Patient;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PatientRepository extends JpaRepository<Patient, Long> {
    @EntityGraph(attributePaths = {"doctors.specialty"})
    Optional<Patient> findPatientDoctorsById(Long id);

    Optional<Patient> findByPhoneNumber(String phoneNumber);

    boolean existsByCi(String ci);

    boolean existsByEmail(String email);

    boolean existsByPhoneNumber(String phoneNumber);
}
