package com.example.scheduler.service.impl;

import com.example.scheduler.config.tenant.TenantContext;
import com.example.scheduler.service.SchemaProvisioningService;
import javax.sql.DataSource;
import lombok.RequiredArgsConstructor;
import org.flywaydb.core.Flyway;
import org.jspecify.annotations.NonNull;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SchemaProvisioningServiceImpl implements SchemaProvisioningService, ApplicationRunner {
    private static final String TENANT_MIGRATIONS = "classpath:db/migration/tenant";

    private final DataSource dataSource;
    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(@NonNull ApplicationArguments args) {
        jdbcTemplate
                .queryForList("SELECT slug FROM clinics", String.class)
                .forEach(slug -> createTenantSchema(TenantContext.schemaName(slug)));
    }

    @Override
    public void createTenantSchema(String schemaName) {
        validateSchemaName(schemaName);
        Flyway.configure()
                .dataSource(dataSource)
                .schemas(schemaName)
                .locations(TENANT_MIGRATIONS)
                .baselineOnMigrate(true)
                .load()
                .migrate();
    }

    @Override
    public void dropTenantSchema(String schemaName) {
        validateSchemaName(schemaName);
        jdbcTemplate.execute("DROP SCHEMA IF EXISTS \"" + schemaName + "\" CASCADE");
    }

    private void validateSchemaName(String schemaName) {
        if (schemaName == null || !schemaName.matches("^clinic_[a-z0-9]{3,50}$"))
            throw new IllegalArgumentException("Invalid tenant schema name: " + schemaName);
    }
}
