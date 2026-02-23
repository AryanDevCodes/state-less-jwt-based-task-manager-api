package com.microservice.taskmanager.controller;

import com.microservice.taskmanager.dto.TokenResponseDto;
import com.microservice.taskmanager.service.RefreshTokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/refresh")
@RequiredArgsConstructor
public class RefreshTokenController {

    private final RefreshTokenService refreshTokenService;

    @PostMapping("/token")
    public ResponseEntity<TokenResponseDto> refreshToken(@RequestBody TokenRequestDto request) {
        TokenResponseDto response = refreshTokenService.rotateToken(request.getRefreshToken());
        return ResponseEntity.ok(response);
    }
}

@lombok.Data
class TokenRequestDto {
    private String refreshToken;
}
