package com.example.scheduler.enums;

public enum AppointmentStatus {
    PENDING, CONFIRMED, CANCELLED;

    public String getDisplayName() {
        return switch (this) {
            case PENDING -> "Pendiente";
            case CONFIRMED -> "Confirmado";
            case CANCELLED -> "Cancelado";
        };
    }
}
