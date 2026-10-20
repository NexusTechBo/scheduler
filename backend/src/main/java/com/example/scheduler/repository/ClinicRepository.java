package com.example.scheduler.repository;

import com.example.scheduler.entity.Clinic;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ClinicRepository extends JpaRepository<Clinic, Long> {
    boolean existsByNameIgnoreCase(String name);
    boolean existsBySubdomain(String subdomain);
    Optional<Clinic> findBySubdomain(String subdomain);
}
