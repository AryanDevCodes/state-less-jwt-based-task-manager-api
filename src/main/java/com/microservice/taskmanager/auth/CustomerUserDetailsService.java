package com.microservice.taskmanager.auth;

import com.microservice.taskmanager.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NullMarked;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomerUserDetailsService implements UserDetailsService {
    private final UserRepository userRepository;

    @Override
    @NullMarked
    public UserDetails loadUserByUsername( String username ) throws UsernameNotFoundException {
        return userRepository.findUserByUsername(username)
                .orElseThrow(
                        () -> new UsernameNotFoundException("User not found with username: " + username)
                );
    }
}
