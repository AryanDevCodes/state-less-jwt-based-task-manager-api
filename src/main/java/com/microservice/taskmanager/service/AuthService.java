package com.microservice.taskmanager.service;

import com.microservice.taskmanager.auth.JwtService;
import com.microservice.taskmanager.dto.LoginDTO;
import com.microservice.taskmanager.dto.LoginResponseDTO;
import com.microservice.taskmanager.dto.RegisterRequestDTO;
import com.microservice.taskmanager.entity.RefreshToken;
import com.microservice.taskmanager.entity.User;
import com.microservice.taskmanager.entity.role.Role;
import com.microservice.taskmanager.repositories.UserRepository;
import com.microservice.taskmanager.service.mapper.UserMapper;
import lombok.RequiredArgsConstructor;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final UserMapper userMapper;
    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final RefreshTokenService refreshTokenService;

    public User registerUser(RegisterRequestDTO dto) {
        User user = userMapper.toEntity(dto);
        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        user.setRole(Role.USER);
        user.setEmail(dto.getEmail());
        return userRepository.save(user);
    }

    public LoginResponseDTO login(LoginDTO dto) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        dto.getEmail(),
                        dto.getPassword()));
        var user = userRepository.findByEmail(dto.getEmail()).orElseThrow(
                () -> new UsernameNotFoundException("user Not found"));
        String token = jwtService.generateToken(user);

        RefreshToken refreshToken = refreshTokenService.createRefreshToken(user);
        LoginResponseDTO responseDTO = userMapper.toResponseDTO(user);
        responseDTO.setToken(token);
        responseDTO.setRefreshToken(refreshToken.getToken());
        return responseDTO;
    }

    public void logout(User user) {
        refreshTokenService.deleteAllUserTokens(user.getId());
    }
}
