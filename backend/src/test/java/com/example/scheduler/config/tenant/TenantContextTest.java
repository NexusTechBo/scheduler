package com.example.scheduler.config.tenant;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TenantContextTest {

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void bindsClinicIdAndSchemaNamedAfterSlug() {
        TenantContext.setCurrentTenant(42L, "clinicasur");
        assertThat(TenantContext.getCurrentClinicId()).isEqualTo(42L);
        assertThat(TenantContext.getCurrentTenant()).isEqualTo("clinic_clinicasur");
    }

    @Test
    void throwsWhenNoClinicTenantIsBound() {
        assertThatThrownBy(TenantContext::getCurrentClinicId).isInstanceOf(IllegalStateException.class);
        assertThat(TenantContext.getCurrentTenant()).isNull();
    }
}
