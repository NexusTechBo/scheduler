package com.example.scheduler.config.tenant;

public class TenantContext {
    private static final String CLINIC_SCHEMA_PREFIX = "clinic_";
    private static final ThreadLocal<String> CURRENT_TENANT = new ThreadLocal<>();

    public static void setCurrentTenant(String tenantId) {
        CURRENT_TENANT.set(tenantId);
    }

    public static String getCurrentTenant() {
        return CURRENT_TENANT.get();
    }

    public static Long getCurrentClinicId() {
        var tenant = getCurrentTenant();
        if (tenant == null || !tenant.startsWith(CLINIC_SCHEMA_PREFIX))
            throw new IllegalStateException("No clinic tenant bound");
        try {
            return Long.valueOf(tenant.substring(CLINIC_SCHEMA_PREFIX.length()));
        } catch (NumberFormatException e) {
            throw new IllegalStateException("No clinic tenant bound", e);
        }
    }

    public static void clear() {
        CURRENT_TENANT.remove();
    }
}
