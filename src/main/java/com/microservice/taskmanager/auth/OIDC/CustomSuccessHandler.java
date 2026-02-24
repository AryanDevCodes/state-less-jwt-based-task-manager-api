package com.microservice.taskmanager.auth.OIDC;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import com.microservice.taskmanager.auth.JwtService;
import com.microservice.taskmanager.entity.User;
import com.microservice.taskmanager.repositories.UserRepository;
import com.microservice.taskmanager.service.RefreshTokenService;
import com.microservice.taskmanager.util.CookieUtil;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@Component
public class CustomSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {
    @Autowired
    private  UserRepository userRepository;
    @Autowired
    private  JwtService jwtService;
    @Autowired
    private  RefreshTokenService refreshTokenService;
    @Autowired
    private  CookieUtil cookieUtil;

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication) throws IOException, ServletException {

        // Extract user details from OIDC token
        DefaultOidcUser oidcUser = (DefaultOidcUser) authentication.getPrincipal();
        String email = oidcUser.getEmail();
        String name = oidcUser.getFullName();

        // Create or update user in database
        User user = userRepository.findByEmail(email)
                .orElseGet(() -> {
                    User newUser = new User();
                    newUser.setEmail(email);
                    newUser.setName(name);
                    newUser.setPassword(null);
                    newUser.setRole(com.microservice.taskmanager.entity.role.Role.USER);
                    return userRepository.save(newUser);
                });

        // Generate JWT tokens
        String accessToken = jwtService.generateToken(user);
        String refreshToken = refreshTokenService.createRefreshToken(user).getToken();

        // Set cookies using CookieUtil
        response.addHeader(HttpHeaders.SET_COOKIE, cookieUtil.createAccessTokenCookie(accessToken).toString());
        response.addHeader(HttpHeaders.SET_COOKIE, cookieUtil.createRefreshTokenCookie(refreshToken).toString());

        // Redirect to frontend with tokens in URL
        String targetUrl = UriComponentsBuilder.fromUriString("http://localhost:8080/")
                .build().toUriString();

        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }
}
