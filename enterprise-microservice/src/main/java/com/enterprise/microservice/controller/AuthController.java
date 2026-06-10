package com.enterprise.microservice.controller;

import com.enterprise.microservice.dto.ApiErrorResponse;
import com.enterprise.microservice.dto.JwtResponse;
import com.enterprise.microservice.dto.LoginRequest;
import com.enterprise.microservice.exception.ErrorCode;
import com.enterprise.microservice.security.JwtTokenProvider;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "JWT login and token management")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider tokenProvider;

    @Operation(
            summary     = "Login and obtain JWT",
            description = "Authenticate with username + password. Returns a signed JWT valid for the configured expiry period."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Login successful",
                    content = @Content(schema = @Schema(implementation = JwtResponse.class))),
            @ApiResponse(responseCode = "401", description = "Invalid credentials",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Account disabled or locked",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    @SecurityRequirements   // This endpoint explicitly requires NO auth — overrides global scheme
    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest loginRequest,
                                   HttpServletRequest httpRequest) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            loginRequest.getUsername(), loginRequest.getPassword()));

            SecurityContextHolder.getContext().setAuthentication(authentication);
            String jwt = tokenProvider.generateToken(authentication);

            log.info("Successful login for user: {}", loginRequest.getUsername());

            // expiresIn in SECONDS (JWT spec), not milliseconds
            return ResponseEntity.ok(new JwtResponse(jwt, "Bearer", tokenProvider.getExpirationInSeconds()));

        } catch (BadCredentialsException e) {
            log.warn("Failed login attempt for user: {}", loginRequest.getUsername());
            ApiErrorResponse error = ApiErrorResponse.of(HttpStatus.UNAUTHORIZED.value(),
                    ErrorCode.ERR_AUTH_001.getCode(), "Invalid username or password.",
                    httpRequest.getRequestURI(), MDC.get("traceId"));
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
        } catch (DisabledException | LockedException e) {
            ApiErrorResponse error = ApiErrorResponse.of(HttpStatus.FORBIDDEN.value(),
                    ErrorCode.ERR_AUTH_004.getCode(), "Account unavailable. Contact support.",
                    httpRequest.getRequestURI(), MDC.get("traceId"));
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
        }
    }
}