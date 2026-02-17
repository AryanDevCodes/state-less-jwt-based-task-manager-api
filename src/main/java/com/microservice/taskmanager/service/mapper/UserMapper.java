package com.microservice.taskmanager.service.mapper;

import com.microservice.taskmanager.dto.LoginResponseDTO;
import com.microservice.taskmanager.dto.RegisterRequestDTO;
import com.microservice.taskmanager.entity.User;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UserMapper {
     User toEntity( RegisterRequestDTO dto );
     LoginResponseDTO toResponseDTO( User user );
}
