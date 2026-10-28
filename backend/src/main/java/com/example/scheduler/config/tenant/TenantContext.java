package com.example.scheduler.config.tenant;

public class TenantContext {
    private static final String CLINIC_SCHEMA_PREFIX = "clinic_";
    private static final ThreadLocal<Tenant> CURRENT_TENANT = new ThreadLocal<>();

    public record Tenant(Long clinicId, String clinicSlug) {}

    public static String schemaName(String clinicSlug) {
        return CLINIC_SCHEMA_PREFIX + clinicSlug;
    }

    public static void setCurrentTenant(Long clinicId, String clinicSlug) {
        CURRENT_TENANT.set(new Tenant(clinicId, clinicSlug));
    }

    public static Tenant current() {
        return CURRENT_TENANT.get();
    }

    public static String getCurrentTenant() {
        var tenant = current();
        return tenant == null ? null : schemaName(tenant.clinicSlug());
    }

    public static Long getCurrentClinicId() {
        var tenant = current();
        if (tenant == null)
            throw new IllegalStateException("No clinic tenant bound");
        return tenant.clinicId();
    }

    public static void clear() {
        CURRENT_TENANT.remove();
    }
}
