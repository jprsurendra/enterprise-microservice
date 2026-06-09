package com.enterprise.microservice.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.User;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTest {

    private JwtService jwtService;

    @BeforeEach
    void setup() {

        jwtService = new JwtService(
                "VGhpc0lzQVNlY3VyZVNlY3JldEtleUZvckpXVFRlc3QxMjM0NTY=",
                3600000
        );
    }

    @Test
    void shouldGenerateValidToken() {

        User user = new User(
                "admin",
                "password",
                Collections.emptyList());

        String token =
                jwtService.generateToken(user);

        assertNotNull(token);

        assertTrue(
                jwtService.validateToken(token));
    }

    @Test
    void shouldExtractUsername() {

        User user = new User(
                "admin",
                "password",
                Collections.emptyList());

        String token =
                jwtService.generateToken(user);

        assertEquals(
                "admin",
                jwtService.extractUsername(token));
    }
}