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
    @Query("SELECT COUNT(c) > 0 FROM Clinic c WHERE LOWER(c.name) = LOWER(:name)")
    boolean existsByNameIgnoreCase(@Param("name") String name);
    boolean existsBySlug(String slug);
    Optional<Clinic> findBySlug(String slug);

    @Query("SELECT c.slug FROM Clinic c WHERE c.id = :id")
    Optional<String> findSlugById(@Param("id") Long id);

    @Query("SELECT c.id FROM Clinic c WHERE c.status = ClinicStatus.PENDING_ACTIVATION AND " +
            "(LOWER(c.name) = LOWER(:name) OR c.slug = :slug)")
    List<Long> findPendingIdsByNameOrSlug(@Param("name") String name, @Param("slug") String slug);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM Clinic c WHERE c.id = :id")
    Optional<Clinic> findByIdForUpdate(@Param("id") Long id);
}
