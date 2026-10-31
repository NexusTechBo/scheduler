package com.example.scheduler.util;

public final class LogKeys {
    public static final String TRACE_ID = "traceId";
    public static final String CLINIC_ID = "clinicId";
    public static final String CLINIC_SLUG = "clinicSlug";
    public static final String USER_ID = "userId";
    public static final String ROLE = "role";
    public static final String GUEST_USER = "guest";
    public static final String REQUEST_ID_HEADER = "X-Request-Id";

    public static final String ATTR_CLINIC_ID = LogKeys.class.getName() + "." + CLINIC_ID;
    public static final String ATTR_USER_ID = LogKeys.class.getName() + "." + USER_ID;
    public static final String ATTR_ROLE = LogKeys.class.getName() + "." + ROLE;

    private LogKeys() {}
}
