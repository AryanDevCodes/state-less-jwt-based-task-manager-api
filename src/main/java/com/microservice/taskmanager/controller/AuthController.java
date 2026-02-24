package com.microservice.taskmanager.controller;

import com.microservice.taskmanager.dto.LoginDTO;
import com.microservice.taskmanager.dto.LoginResponseDTO;
import com.microservice.taskmanager.dto.RegisterRequestDTO;
import com.microservice.taskmanager.entity.User;
import com.microservice.taskmanager.service.AuthService;
import com.microservice.taskmanager.util.CookieUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final CookieUtil cookieUtil;

    @PostMapping("/register")
    public ResponseEntity<User> register(@RequestBody RegisterRequestDTO request) {
        return ResponseEntity.ok(authService.registerUser(request));
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponseDTO> authenticate(@RequestBody LoginDTO request) {
        LoginResponseDTO response = authService.login(request);

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookieUtil.createAccessTokenCookie(response.getToken()).toString())
                .header(HttpHeaders.SET_COOKIE,
                        cookieUtil.createRefreshTokenCookie(response.getRefreshToken()).toString())
                .body(response);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@AuthenticationPrincipal User user) {
        // Delete all refresh tokens from database
        if (user != null) {
            authService.logout(user);
        }

        // Clear cookies
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookieUtil.deleteAccessTokenCookie().toString())
                .header(HttpHeaders.SET_COOKIE, cookieUtil.deleteRefreshTokenCookie().toString())
                .build();
    }
}
