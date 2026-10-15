package com.example.scheduler.service.impl;

import com.example.scheduler.service.SchemaProvisioningService;
import lombok.RequiredArgsConstructor;
import org.flywaydb.core.Flyway;
import org.jspecify.annotations.NonNull;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;

@Service
@RequiredArgsConstructor
public class SchemaProvisioningServiceImpl implements SchemaProvisioningService, ApplicationRunner {
    private static final String TENANT_MIGRATIONS = "classpath:db/migration/tenant";

    private final DataSource dataSource;
    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(@NonNull ApplicationArguments args) {
        jdbcTemplate.queryForList("SELECT id FROM public.clinics", Long.class)
                .forEach(id -> createTenantSchema("clinic_" + id));
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

    private void validateSchemaName(String schemaName) {
        if (schemaName == null || !schemaName.matches("^clinic_\\d+$"))
            throw new IllegalArgumentException("Invalid tenant schema name: " + schemaName);
    }
}
