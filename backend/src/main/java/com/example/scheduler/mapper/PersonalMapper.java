package com.example.scheduler.mapper;

import com.example.scheduler.dto.personal.PersonalResponse;
import com.example.scheduler.entity.Personal;
import java.util.List;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface PersonalMapper {
    @Mapping(target = "specialtyName", source = "specialty.name")
    PersonalResponse toResponse(Personal personal);

    List<PersonalResponse> toResponseList(List<Personal> personals);
}
