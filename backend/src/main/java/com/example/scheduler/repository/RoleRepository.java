package com.example.scheduler.repository;

import com.example.scheduler.entity.Role;
import com.example.scheduler.enums.ERole;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoleRepository extends JpaRepository<Role, Long> {
    Role getByName(ERole name);
}
