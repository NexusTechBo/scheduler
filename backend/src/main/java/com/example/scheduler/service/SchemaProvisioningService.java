package com.example.scheduler.service;

public interface SchemaProvisioningService {
    void createTenantSchema(String schemaName);
    /** Drops the tenant schema and everything in it. Joins the caller's transaction when there is one. */
    void dropTenantSchema(String schemaName);
}
