package com.enterprise.microservice.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    // For demo purposes - in production, fetch from database
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {

        // This is a sample user - replace with database lookup
        if ("admin".equals(username)) {
            return CustomUserDetails.builder()
                    .id(1L)
                    .username("admin")
                    .email("admin@enterprise.com")
                    .password("$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG") // password: admin123
                    .roles(Set.of("ADMIN", "USER"))
                    .active(true)
                    .build();
        } else if ("user".equals(username)) {
            return CustomUserDetails.builder()
                    .id(2L)
                    .username("user")
                    .email("user@enterprise.com")
                    .password("$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG") // password: admin123
                    .roles(Set.of("USER"))
                    .active(true)
                    .build();
        }

        throw new UsernameNotFoundException("User not found with username: " + username);
    }
}