package com.example.scheduler.config;

import com.example.scheduler.tenant.CustomTenantResolver;
import com.example.scheduler.tenant.SchemaBasedMultiTenantConnectionProvider;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.hibernate.cfg.AvailableSettings;
import org.springframework.boot.hibernate.autoconfigure.HibernatePropertiesCustomizer;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class HibernateConfig implements HibernatePropertiesCustomizer {
    private final SchemaBasedMultiTenantConnectionProvider connectionProvider;
    private final CustomTenantResolver tenantResolver;

    @Override
    public void customize(Map<String, Object> hibernateProperties) {
        hibernateProperties.put(AvailableSettings.MULTI_TENANT_CONNECTION_PROVIDER, connectionProvider);
        hibernateProperties.put(AvailableSettings.MULTI_TENANT_IDENTIFIER_RESOLVER, tenantResolver);
    }
}
