package com.microservice.taskmanager.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * DTO for {@link com.microservice.taskmanager.entity.RefreshToken}
 */
@Data
public class TokenResponseDto implements Serializable {
    String accessToken;
    String refreshToken;

    public TokenResponseDto( String newAccessToken, String token ) {
        this.accessToken = newAccessToken;
        this.refreshToken = token;
    }
}