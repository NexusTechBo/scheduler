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
    void parsesClinicIdFromTenantSchema() {
        TenantContext.setCurrentTenant("clinic_42");
        assertThat(TenantContext.getCurrentClinicId()).isEqualTo(42L);
    }

    @Test
    void throwsWhenNoClinicTenantIsBound() {
        assertThatThrownBy(TenantContext::getCurrentClinicId).isInstanceOf(IllegalStateException.class);
        TenantContext.setCurrentTenant("public");
        assertThatThrownBy(TenantContext::getCurrentClinicId).isInstanceOf(IllegalStateException.class);
        TenantContext.setCurrentTenant("clinic_abc");
        assertThatThrownBy(TenantContext::getCurrentClinicId).isInstanceOf(IllegalStateException.class);
    }
}
