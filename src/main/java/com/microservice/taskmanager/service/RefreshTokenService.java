package com.microservice.taskmanager.service;

import com.microservice.taskmanager.auth.JwtService;
import com.microservice.taskmanager.dto.TokenResponseDto;
import com.microservice.taskmanager.entity.RefreshToken;
import com.microservice.taskmanager.entity.RefreshTokenRepository;
import com.microservice.taskmanager.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtService jwtService;
    @Value("${jwt.refresh-token.expiration}")
    private long refreshExpiration;

    public RefreshToken verifyExpiration(RefreshToken token) {
        if (token.getExpiryTime().isBefore(Instant.now())) {
            refreshTokenRepository.delete(token);
            throw new RuntimeException("Refresh Token was expired, Please SignIn again. ");
        }
        return token;
    }

    @Transactional
    public TokenResponseDto rotateToken(String requestToken) {
        return refreshTokenRepository.findByToken(requestToken)
                .map(this::verifyExpiration)
                .filter(token -> !token.isRevoked())
                .map(token -> {
                    User user = token.getUser();
                    token.setRevoked(true);
                    refreshTokenRepository.delete(token);
                    String newAccessToken = jwtService.generateToken(user);
                    RefreshToken refreshToken = createRefreshToken(user);
                    return new TokenResponseDto(newAccessToken, refreshToken.getToken());
                })
                .orElseThrow(
                        () -> new RuntimeException(
                                "Refresh token is invalid or expired"));
    }

    public RefreshToken createRefreshToken(User user) {
        RefreshToken refreshToken = new RefreshToken();

        refreshToken.setUser(user);
        refreshToken.setToken(UUID.randomUUID().toString());
        refreshToken.setExpiryTime(Instant.now().plusMillis(refreshExpiration));
        refreshToken.setRevoked(false);

        return refreshTokenRepository.save(refreshToken);
    }

}
