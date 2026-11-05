package com.example.scheduler.service;

public interface SchemaProvisioningService {
    void createTenantSchema(String schemaName);

    void dropTenantSchema(String schemaName);
}
