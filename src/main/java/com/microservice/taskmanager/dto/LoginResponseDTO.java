package com.microservice.taskmanager.dto;

import lombok.Data;

@Data
public class LoginResponseDTO {
    String email;
    String token;
    private String refreshToken;
}
