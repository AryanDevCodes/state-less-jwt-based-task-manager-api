package com.microservice.taskmanager.config;

import com.microservice.taskmanager.auth.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter authenticationFilter;


    @Bean
    public SecurityFilterChain securityFilterChain( HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(
                        session -> session.
                                sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                        )
                .authorizeHttpRequests(
                        auth -> auth
                                .requestMatchers("/api/auth/**").permitAll()
                                .anyRequest().authenticated()
                )
                .addFilterBefore(
                        authenticationFilter, UsernamePasswordAuthenticationFilter.class
                );
        return http.build();
    }
}
