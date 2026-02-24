package com.microservice.taskmanager.service.mapper;

import com.microservice.taskmanager.dto.TaskRequestDto;
import com.microservice.taskmanager.dto.TaskResponseDto;
import com.microservice.taskmanager.entity.Task;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring")
public interface TaskMapper {
    @Mapping(target = "user", ignore = true)
    Task toEntity(TaskRequestDto taskRequestDto);

    TaskResponseDto toResponseDto(Task task);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "user", ignore = true)
    Task partialUpdate(TaskRequestDto taskRequestDto, @MappingTarget Task task);
}
