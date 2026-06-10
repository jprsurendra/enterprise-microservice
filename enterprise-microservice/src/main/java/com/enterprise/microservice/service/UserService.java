package com.enterprise.microservice.service;

import com.enterprise.microservice.dto.RegisterRequest;
import com.enterprise.microservice.dto.UserResponse;
import com.enterprise.microservice.entity.Role;
import com.enterprise.microservice.entity.User;
import com.enterprise.microservice.exception.BusinessException;
import com.enterprise.microservice.exception.ErrorCode;
import com.enterprise.microservice.repository.RoleRepository;
import com.enterprise.microservice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private static final String DEFAULT_ROLE = "ROLE_USER";

    private final UserRepository   userRepository;
    private final RoleRepository   roleRepository;
    private final PasswordEncoder  passwordEncoder;

    /**
     * Registers a new user.
     *
     * Steps:
     *  1. Guard: reject duplicate username or email.
     *  2. Hash the plain-text password with BCrypt(12).
     *  3. Assign the default ROLE_USER (fetched from DB — not hardcoded string).
     *  4. Persist and return a sanitized UserResponse (no password field).
     */
    @Transactional
    public UserResponse register(RegisterRequest request) {

        // Duplicate username check
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new BusinessException(ErrorCode.ERR_DATA_CONFLICT,
                    "Username '" + request.getUsername() + "' is already taken.");
        }

        // Duplicate email check
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BusinessException(ErrorCode.ERR_DATA_CONFLICT,
                    "Email '" + request.getEmail() + "' is already registered.");
        }

        // Resolve default role from DB — fail fast if seed data is missing
        Role defaultRole = roleRepository.findByName(DEFAULT_ROLE)
                .orElseThrow(() -> new IllegalStateException(
                        "Default role '" + DEFAULT_ROLE + "' not found in database. " +
                                "Ensure the roles seed SQL has been executed."));

        User user = new User(
                request.getUsername(),
                request.getEmail(),
                passwordEncoder.encode(request.getPassword()),  // BCrypt(12)
                request.getFullName()
        );
        user.setRoles(Set.of(defaultRole));

        User saved = userRepository.save(user);
        log.info("New user registered — id={} username={}", saved.getId(), saved.getUsername());

        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public UserResponse getUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.ERR_DATA_NOT_FOUND,
                        "User not found with id: " + id));
        return toResponse(user);
    }

    @Transactional(readOnly = true)
    public UserResponse getUserByUsername(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new BusinessException(ErrorCode.ERR_DATA_NOT_FOUND,
                        "User not found: " + username));
        return toResponse(user);
    }

    // -----------------------------------------------------------------------
    // Mapping — User entity → UserResponse DTO (password is never included)
    // -----------------------------------------------------------------------

    private UserResponse toResponse(User user) {
        Set<String> roleNames = user.getRoles().stream()
                .map(Role::getName)
                .collect(Collectors.toUnmodifiableSet());

        return UserResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .roles(roleNames)
                .active(user.getActive())
                .createdAt(user.getCreatedAt())
                .build();
    }
}