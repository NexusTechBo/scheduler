package com.example.scheduler.enums;

import lombok.Getter;

@Getter
public enum ESpecialty {
    DEFAULT(1L);

    private final Long id;

    ESpecialty(Long id) {
        this.id = id;
    }

    public String getDisplayName() {
        return switch (this) {
            case DEFAULT -> "None";
        };
    }
}
