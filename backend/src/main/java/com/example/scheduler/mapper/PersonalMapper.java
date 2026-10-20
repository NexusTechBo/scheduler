package com.example.scheduler.mapper;

import com.example.scheduler.dto.personal.PersonalRequest;
import com.example.scheduler.dto.personal.PersonalResponse;
import com.example.scheduler.dto.personal.ProfileResponse;
import com.example.scheduler.entity.Personal;
import org.mapstruct.*;

import java.util.List;

@Mapper(componentModel = "spring")
public interface PersonalMapper {
    @Mapping(target = "specialtyName", source = "specialty.name")
    PersonalResponse toResponse(Personal personal);
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "username", ignore = true)
    @Mapping(target = "ci", ignore = true)
    @Mapping(target = "password", ignore = true)
    @Mapping(target = "role", ignore = true)
    @Mapping(target = "active", ignore = true)
    @Mapping(target = "specialty", ignore = true)
    @Mapping(target = "patients", ignore = true)
    void toEntityUpdated(PersonalRequest request, @MappingTarget Personal personal);
    List<PersonalResponse> toResponseList(List<Personal> personals);
}
