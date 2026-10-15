package com.example.scheduler.mapper;

import com.example.scheduler.dto.ScheduleException.ScheduleExceptionRequest;
import com.example.scheduler.dto.ScheduleException.ScheduleExceptionResponse;
import com.example.scheduler.entity.ScheduleException;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ScheduleExceptionMapper {
    ScheduleExceptionResponse toResponse(ScheduleException scheduleException);
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "doctor", ignore = true)
    ScheduleException toEntity(ScheduleExceptionRequest scheduleExceptionResponse);
}
