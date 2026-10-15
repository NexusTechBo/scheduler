package com.example.scheduler.enums;

import lombok.Getter;

@Getter
public enum ERole {
    ADMINISTRATOR(1L),
    DOCTOR(2L),
    ASSISTANT(3L),
    PATIENT(4L);

    private final Long id;

    ERole(Long id) {
        this.id = id;
    }

    public String getDisplayName() {
        return switch (this) {
            case ADMINISTRATOR -> "Administrador";
            case DOCTOR -> "Doctor";
            case ASSISTANT -> "Asistente";
            case PATIENT -> "Paciente";
        };
    }
}
