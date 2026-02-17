package com.microservice.taskmanager.controller;

import com.microservice.taskmanager.dto.LoginDTO;
import com.microservice.taskmanager.dto.LoginResponseDTO;
import com.microservice.taskmanager.dto.RegisterRequestDTO;
import com.microservice.taskmanager.entity.User;
import com.microservice.taskmanager.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService; 

    @PostMapping("/register")
    public ResponseEntity<User> register( @RequestBody RegisterRequestDTO request) {
        return ResponseEntity.ok(authService.registerUser(request));
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponseDTO> authenticate(@RequestBody LoginDTO request) {
        return ResponseEntity.ok(authService.login(request));
    }
}
