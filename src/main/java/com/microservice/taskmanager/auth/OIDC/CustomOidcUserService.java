package com.microservice.taskmanager.auth.OIDC;

import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;

import com.microservice.taskmanager.entity.User;
import com.microservice.taskmanager.entity.role.Role;
import com.microservice.taskmanager.repositories.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CustomOidcUserService extends OidcUserService {
    private final UserRepository userRepository;

    @Override
    public OidcUser loadUser(OidcUserRequest request) {
        OidcUser oidcUser = super.loadUser(request);
        String email = oidcUser.getEmail();
        String name = oidcUser.getFullName();

        userRepository.findByEmail(email)
            .orElseGet(()-> {
                User user = new User();
                user.setEmail(email);
                user.setName(name);
                user.setRole(Role.USER);
                return userRepository.save(user);
            });

            return oidcUser;
    }

}
