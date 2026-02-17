package com.microservice.taskmanager.service;

import com.microservice.taskmanager.auth.JwtService;
import com.microservice.taskmanager.dto.LoginDTO;
import com.microservice.taskmanager.dto.LoginResponseDTO;
import com.microservice.taskmanager.dto.RegisterRequestDTO;
import com.microservice.taskmanager.entity.User;
import com.microservice.taskmanager.entity.role.Role;
import com.microservice.taskmanager.repositories.UserRepository;
import com.microservice.taskmanager.service.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final UserMapper userMapper;
    private AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final UserRepository userRepository;
    private  PasswordEncoder passwordEncoder;

    public User registerUser( RegisterRequestDTO dto ){
        User user = userMapper.toEntity(dto);
        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        user.setRole(Role.USER);
        return userRepository.save(user);
    }

    public LoginResponseDTO login( LoginDTO dto ){
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        dto.getEmail(),
                        dto.getPassword()
                )
        );
        Optional<User> user = userRepository.findUserByUsername(dto.getEmail());
        String token = jwtService.generateToken(user.get());

        LoginResponseDTO responseDTO = userMapper.toResponseDTO(user.get());

        responseDTO.setToken(token);

        return responseDTO;
    }

}
