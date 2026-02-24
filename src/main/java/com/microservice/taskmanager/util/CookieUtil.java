package com.microservice.taskmanager.util;

import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

@Component
public class CookieUtil {

    private static final int ACCESS_TOKEN_MAX_AGE = 180; // 1 hour
    private static final int REFRESH_TOKEN_MAX_AGE = 604800; // 7 days

    public ResponseCookie createAccessTokenCookie(String token) {
        return ResponseCookie.from("accessToken", token)
                .httpOnly(false)
                .secure(false) // Set to true in production with HTTPS
                .path("/")
                .maxAge(ACCESS_TOKEN_MAX_AGE)
                .sameSite("Lax")
                .build();
    }

    public ResponseCookie createRefreshTokenCookie(String token) {
        return ResponseCookie.from("refreshToken", token)
                .httpOnly(false)
                .secure(false) // Set to true in production with HTTPS
                .path("/")
                .maxAge(REFRESH_TOKEN_MAX_AGE)
                .sameSite("Lax")
                .build();
    }

    public ResponseCookie deleteAccessTokenCookie() {
        return ResponseCookie.from("accessToken", "")
                .httpOnly(false)
                .secure(false)
                .path("/")
                .maxAge(0)
                .build();
    }

    public ResponseCookie deleteRefreshTokenCookie() {
        return ResponseCookie.from("refreshToken", "")
                .httpOnly(false)
                .secure(false)
                .path("/")
                .maxAge(0)
                .build();
    }
}
