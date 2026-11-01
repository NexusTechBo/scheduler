package com.example.scheduler.security;

import java.util.List;

public final class RoleExpressions {
    public static final List<String> STAFF_ROLES = List.of("ADMINISTRATOR", "DOCTOR", "ASSISTANT");

    public static final String STAFF = "hasAnyRole('ADMINISTRATOR', 'DOCTOR', 'ASSISTANT')";
    public static final String ADMINISTRATOR = "hasRole('ADMINISTRATOR')";
    public static final String ADMINISTRATOR_OR_ASSISTANT = "hasAnyRole('ADMINISTRATOR', 'ASSISTANT')";
    public static final String DOCTOR = "hasRole('DOCTOR')";
    public static final String GUEST = "hasRole('GUEST')";

    private RoleExpressions() {}
}
