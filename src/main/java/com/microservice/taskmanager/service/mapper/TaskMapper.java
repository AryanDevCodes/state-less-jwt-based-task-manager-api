package com.microservice.taskmanager.service.mapper;

import com.microservice.taskmanager.dto.TaskRequestDto;
import com.microservice.taskmanager.dto.TaskResponseDto;
import com.microservice.taskmanager.entity.Task;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring")
public interface TaskMapper {
    Task toEntity( TaskRequestDto taskRequestDto );

    TaskResponseDto toResponseDto( Task task );

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    Task partialUpdate( TaskRequestDto taskRequestDto, @MappingTarget Task task );
}
