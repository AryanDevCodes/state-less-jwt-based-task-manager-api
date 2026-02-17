package com.microservice.taskmanager.dto;

import com.microservice.taskmanager.entity.role.Role;
import lombok.Data;

@Data
public class RegisterRequestDTO {
    private String email;
    private String password;
    private Role role;
}
