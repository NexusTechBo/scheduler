package com.example.scheduler.mapper;

import com.example.scheduler.dto.specialty.SpecialtyRequest;
import com.example.scheduler.dto.specialty.SpecialtyResponse;
import com.example.scheduler.entity.Specialty;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface SpecialtyMapper {
    List<SpecialtyResponse> toResponseList(List<Specialty> specialties);
    SpecialtyResponse toResponse(Specialty specialty);
    @Mapping(target = "id", ignore = true)
    Specialty toEntity(SpecialtyRequest request);
}
