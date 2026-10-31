package com.example.scheduler.repository;

import com.example.scheduler.entity.ClinicActivationToken;
import jakarta.persistence.LockModeType;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ClinicActivationTokenRepository extends JpaRepository<ClinicActivationToken, Long> {
    @Query("SELECT t.clinic.id FROM ClinicActivationToken t WHERE t.tokenHash = :tokenHash")
    Optional<Long> findClinicIdByTokenHash(@Param("tokenHash") String tokenHash);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT t FROM ClinicActivationToken t WHERE t.tokenHash = :tokenHash")
    Optional<ClinicActivationToken> findByTokenHashForUpdate(@Param("tokenHash") String tokenHash);

    Optional<ClinicActivationToken> findFirstByClinicIdOrderByCreatedAtDesc(Long clinicId);

    boolean existsByClinicId(Long clinicId);

    boolean existsByClinicIdAndUsedAtIsNullAndExpiresAtAfter(Long clinicId, LocalDateTime now);

    @Query("SELECT DISTINCT t.clinic.id FROM ClinicActivationToken t " + "WHERE LOWER(t.email) = LOWER(:email) AND "
            + "t.clinic.status = ClinicStatus.PENDING_ACTIVATION")
    List<Long> findPendingClinicIdsByEmail(@Param("email") String email);

    @Modifying(flushAutomatically = true)
    @Query("UPDATE ClinicActivationToken t SET t.usedAt = :usedAt WHERE t.clinic.id = :clinicId AND t.usedAt IS NULL")
    int markUnusedAsUsedByClinicId(@Param("clinicId") Long clinicId, @Param("usedAt") LocalDateTime usedAt);
}
