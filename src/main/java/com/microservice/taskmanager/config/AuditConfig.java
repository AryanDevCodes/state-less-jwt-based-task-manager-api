package com.microservice.taskmanager.config;

import java.util.Optional;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

@EnableJpaAuditing
@Configuration
public class AuditConfig {

    @Bean
    public AuditorAware<String> auditorProvidor() {
        return () -> {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated() || authentication.equals("anonymousUser")) {
                return Optional.empty();
            }
            return Optional.ofNullable(authentication.getName());
        };
    }
}
