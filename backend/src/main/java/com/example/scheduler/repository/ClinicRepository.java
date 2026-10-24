package com.example.scheduler.repository;

import com.example.scheduler.entity.Clinic;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ClinicRepository extends JpaRepository<Clinic, Long> {
    /** LOWER on both sides, the same comparison as {@link #findPendingIdsByNameOrSubdomain}. */
    @Query("SELECT COUNT(c) > 0 FROM Clinic c WHERE LOWER(c.name) = LOWER(:name)")
    boolean existsByNameIgnoreCase(@Param("name") String name);
    boolean existsBySubdomain(String subdomain);
    Optional<Clinic> findBySubdomain(String subdomain);

    @Query("SELECT c.id FROM Clinic c WHERE c.status = ClinicStatus.PENDING_ACTIVATION AND " +
            "(LOWER(c.name) = LOWER(:name) OR c.subdomain = :subdomain)")
    List<Long> findPendingIdsByNameOrSubdomain(@Param("name") String name, @Param("subdomain") String subdomain);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM Clinic c WHERE c.id = :id")
    Optional<Clinic> findByIdForUpdate(@Param("id") Long id);
}
