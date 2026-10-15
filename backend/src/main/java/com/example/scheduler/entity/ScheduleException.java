package com.example.scheduler.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "id")
@Entity
@Table(name = "schedule_exceptions")
public class ScheduleException {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "doctor_id")
    private Personal doctor;

    @Column(nullable = false)
    private LocalDate date;

    @Column(nullable = false)
    private LocalTime startTime;

    @Column(nullable = false)
    private LocalTime endTime;

    @Column(nullable = false)
    private Boolean isFullDayBlock;

    @Column(nullable = true)
    private String reason;

    /** True when this exception blocks any part of [start, end): a full-day block, or a partial block overlapping it. */
    public boolean blocks(LocalTime start, LocalTime end) {
        if (Boolean.TRUE.equals(isFullDayBlock)) return true;
        return startTime != null && endTime != null && start.isBefore(endTime) && end.isAfter(startTime);
    }
}
