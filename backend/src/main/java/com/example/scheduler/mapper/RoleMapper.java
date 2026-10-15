package com.example.scheduler.mapper;

import com.example.scheduler.dto.role.RoleResponse;
import com.example.scheduler.entity.Role;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface RoleMapper {
    @Mapping(target = "name", expression = "java(role.getName().getDisplayName())")
    RoleResponse toResponse(Role role);
    List<RoleResponse> toResponseList(List<Role> roles);
}
