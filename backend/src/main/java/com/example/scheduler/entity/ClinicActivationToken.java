package com.example.scheduler.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "id")
@Entity
@Table(name = "clinic_activation_tokens", schema = "public")
public class ClinicActivationToken {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "clinic_id", updatable = false)
    private Clinic clinic;

    @Column(nullable = false, unique = true, updatable = false, length = 64)
    private String tokenHash;

    @Column(nullable = false, updatable = false)
    private String email;

    @Column(nullable = false, updatable = false)
    private LocalDateTime expiresAt;

    private LocalDateTime usedAt;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public boolean isUsable(LocalDateTime now) {
        return usedAt == null && now.isBefore(expiresAt);
    }
}
