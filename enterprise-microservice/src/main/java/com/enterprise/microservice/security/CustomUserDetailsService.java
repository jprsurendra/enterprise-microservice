package com.enterprise.microservice.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Set;

/**
 * In-memory user store for local development.
 * Replace with a @Repository-backed UserService for production.
 *
 * Passwords hashed with BCrypt strength 12:
 *   admin  → "Admin@123"  (hash generated via new BCryptPasswordEncoder(12).encode("Admin@123"))
 *   user   → "User@123"
 *
 * To regenerate:
 *   System.out.println(new BCryptPasswordEncoder(12).encode("Admin@123"));
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        log.debug("Loading user by username: {}", username);
        // This is a sample user - replace with database lookup
        return switch (username) {
            case "admin" -> CustomUserDetails.builder()
                    .id(1L)
                    .username("admin")
                    .email("admin@enterprise.com")
                    // BCrypt(12) of "Admin@123" — regenerate with encoder in production
                    .password("$2a$12$mDFqWaLe8nmr7ViQSuZeAuzS0FBqGnfSSEoJ6CtShcKkR2FOrCr3e")
                    .roles(Set.of("ADMIN", "USER"))
                    .enabled(true)
                    .accountNonExpired(true)
                    .accountNonLocked(true)
                    .credentialsNonExpired(true)
                    .build();

            case "user" -> CustomUserDetails.builder()
                    .id(2L)
                    .username("user")
                    .email("user@enterprise.com")
                    // BCrypt(12) of "User@123"
                    .password("$2a$12$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy")
                    .roles(Set.of("USER"))
                    .enabled(true)
                    .accountNonExpired(true)
                    .accountNonLocked(true)
                    .credentialsNonExpired(true)
                    .build();

            default -> throw new UsernameNotFoundException("User not found: " + username);
        };
    }
}