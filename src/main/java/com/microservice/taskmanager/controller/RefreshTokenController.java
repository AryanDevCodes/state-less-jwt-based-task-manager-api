package com.microservice.taskmanager.controller;

import com.microservice.taskmanager.dto.TokenResponseDto;
import com.microservice.taskmanager.service.RefreshTokenService;
import com.microservice.taskmanager.util.CookieUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/refresh")
@RequiredArgsConstructor
public class RefreshTokenController {

    private final RefreshTokenService refreshTokenService;
    private final CookieUtil cookieUtil;

    @PostMapping("/token")
    public ResponseEntity<TokenResponseDto> refreshToken(@RequestBody TokenRequestDto request) {
        TokenResponseDto response = refreshTokenService.rotateToken(request.getRefreshToken());

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE,
                        cookieUtil.createAccessTokenCookie(response.getAccessToken()).toString())
                .header(HttpHeaders.SET_COOKIE,
                        cookieUtil.createRefreshTokenCookie(response.getRefreshToken()).toString())
                .body(response);
    }
}

@lombok.Data
class TokenRequestDto {
    private String refreshToken;
}
