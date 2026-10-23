package com.example.scheduler.entity;

import com.example.scheduler.enums.ClinicStatus;
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
@Table(name = "clinics", schema = "public")
public class Clinic {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String phoneNumber;

    @Column(nullable = false, unique = true, updatable = false)
    private String subdomain;

    /** Defaults to ACTIVE (seeded/legacy clinics); self-registered clinics are set to PENDING_ACTIVATION explicitly. */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private ClinicStatus status = ClinicStatus.ACTIVE;

    private LocalDateTime activatedAt;
}
