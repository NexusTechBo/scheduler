package com.example.scheduler.mapper;

import com.example.scheduler.dto.ScheduleException.ScheduleExceptionResponse;
import com.example.scheduler.entity.ScheduleException;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface ScheduleExceptionMapper {
    ScheduleExceptionResponse toResponse(ScheduleException scheduleException);
    List<ScheduleExceptionResponse> toResponseList(List<ScheduleException> scheduleException);
}
