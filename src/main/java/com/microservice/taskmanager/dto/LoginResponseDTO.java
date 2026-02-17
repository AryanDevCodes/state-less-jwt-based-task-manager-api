package com.microservice.taskmanager.dto;

import lombok.Data;

@Data
public class LoginResponseDTO {
    String token;
    Long expirationDate;
}
