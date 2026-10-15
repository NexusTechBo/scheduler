package com.example.scheduler.mapper;

import com.example.scheduler.dto.personal.PersonalRegisterRequest;
import com.example.scheduler.dto.personal.PersonalRequest;
import com.example.scheduler.dto.personal.PersonalResponse;
import com.example.scheduler.entity.Personal;
import org.mapstruct.*;

import java.util.List;

@Mapper(componentModel = "spring")
public interface PersonalMapper {
    @Mapping(target = "specialtyName", source = "specialty.name")
    @Mapping(target = "roleName", source = "role.name")
    @Mapping(target = "name", source = "account.name")
    @Mapping(target = "email", source = "account.email")
    PersonalResponse toResponse(Personal personal);
    @Mapping(target = "account.name", source = "name")
    @Mapping(target = "account.email", source = "email")
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "role", ignore = true)
    @Mapping(target = "active", ignore = true)
    @Mapping(target = "specialty", ignore = true)
    @Mapping(target = "patients", ignore = true)
    void toEntityUpdated(PersonalRequest request, @MappingTarget Personal personal);
    List<PersonalResponse> toResponseList(List<Personal> personals);
}
