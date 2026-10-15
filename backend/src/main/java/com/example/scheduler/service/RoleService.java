package com.example.scheduler.service;

import com.example.scheduler.dto.role.RoleResponse;

import java.util.List;

public interface RoleService {
    List<RoleResponse> findAllRoles();
}
