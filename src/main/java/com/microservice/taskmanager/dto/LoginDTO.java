package com.microservice.taskmanager.dto;

import lombok.Data;

@Data
public class LoginDTO {
    private String email;
    private String password;
}
