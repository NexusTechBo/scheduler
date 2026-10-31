package com.example.scheduler.mapper;

import com.example.scheduler.dto.specialty.SpecialtyResponse;
import com.example.scheduler.entity.Specialty;
import java.util.List;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface SpecialtyMapper {
    List<SpecialtyResponse> toResponseList(List<Specialty> specialties);

    SpecialtyResponse toResponse(Specialty specialty);
}
