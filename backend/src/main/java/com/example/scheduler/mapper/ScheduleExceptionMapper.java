package com.example.scheduler.mapper;

import com.example.scheduler.dto.scheduleException.ScheduleExceptionResponse;
import com.example.scheduler.entity.ScheduleException;
import java.util.List;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ScheduleExceptionMapper {
    ScheduleExceptionResponse toResponse(ScheduleException scheduleException);

    List<ScheduleExceptionResponse> toResponseList(List<ScheduleException> scheduleException);
}
