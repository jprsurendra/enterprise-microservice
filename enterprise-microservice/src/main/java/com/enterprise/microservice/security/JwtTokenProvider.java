package com.enterprise.microservice.security;

import com.enterprise.microservice.exception.BusinessException;
import com.enterprise.microservice.exception.ErrorCode;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;

/**
 * JWT utility for generation, validation, and claims extraction.
 *
 * Key design decisions:
 * - Issuer claim (iss) is set and verified to prevent cross-service token reuse.
 * - Roles stored as a proper List claim, not a comma-delimited string.
 * - Distinct exception handling per failure mode (expired vs. malformed vs. invalid sig).
 * - Key validated at startup — the application fails fast if the secret is too short.
 */
@Slf4j
@Component
public class JwtTokenProvider {

    private static final String CLAIM_USERNAME  = "username";
    private static final String CLAIM_EMAIL     = "email";
    private static final String CLAIM_ROLES     = "roles";
    private static final int    MIN_KEY_BYTES   = 32;  // 256 bits minimum for HS256

    @Value("${spring.security.jwt.secret}")
    private String jwtSecret;

    @Value("${spring.security.jwt.expiration-ms:3600000}")
    private long jwtExpirationMs;

    @Value("${spring.security.jwt.issuer:enterprise-microservice}")
    private String jwtIssuer;

    private SecretKey secretKey;

    @PostConstruct
    public void init() {
        byte[] keyBytes = jwtSecret.getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < MIN_KEY_BYTES) {
            throw new IllegalStateException(
                    "JWT secret must be at least " + MIN_KEY_BYTES + " bytes (" + MIN_KEY_BYTES * 8 + " bits). " +
                            "Current length: " + keyBytes.length + " bytes. " +
                            "Generate with: openssl rand -base64 32"
            );
        }
        this.secretKey = Keys.hmacShaKeyFor(keyBytes);
        log.info("JwtTokenProvider initialized — issuer={}, expiryMs={}", jwtIssuer, jwtExpirationMs);
    }

    /**
     * Generates a signed JWT for the authenticated principal.
     * Token contains: sub (userId), username, email, roles (List), iss, iat, exp.
     */
    public String generateToken(Authentication authentication) {
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        Date now = new Date();
        Date expiry = new Date(now.getTime() + jwtExpirationMs);

        List<String> roles = userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .toList();

        return Jwts.builder()
                .subject(userDetails.getId().toString())
                .claim(CLAIM_USERNAME, userDetails.getUsername())
                .claim(CLAIM_EMAIL, userDetails.getEmail())
                .claim(CLAIM_ROLES, roles)
                .issuer(jwtIssuer)
                .issuedAt(now)
                .expiration(expiry)
                .signWith(secretKey)
                .compact();
    }

    /**
     * Returns the expiration time in SECONDS (JWT standard unit for 'exp'),
     * suitable for the 'expires_in' field in OAuth2-style token responses.
     */
    public long getExpirationInSeconds() {
        return jwtExpirationMs / 1000;
    }

    public String getUsernameFromToken(String token) {
        return parseClaims(token).get(CLAIM_USERNAME, String.class);
    }

    public Long getUserIdFromToken(String token) {
        return Long.parseLong(parseClaims(token).getSubject());
    }

    @SuppressWarnings("unchecked")
    public List<String> getRolesFromToken(String token) {
        return parseClaims(token).get(CLAIM_ROLES, List.class);
    }

    /**
     * Validates a JWT token. Returns true only if the token is well-formed,
     * properly signed, not expired, and issued by the expected issuer.
     *
     * Logs at appropriate levels: WARN for expected client errors (expired/malformed),
     * ERROR for unexpected failures (bad signature = possible attack).
     */
    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                    .verifyWith(secretKey)
                    .requireIssuer(jwtIssuer)
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (ExpiredJwtException e) {
            log.warn("JWT expired at {}: {}", e.getClaims().getExpiration(), e.getMessage());
            throw new BusinessException(ErrorCode.ERR_AUTH_002);
        } catch (SignatureException e) {
            log.error("JWT signature validation failed — possible tampering attempt: {}", e.getMessage());
            throw new BusinessException(ErrorCode.ERR_AUTH_003);
        } catch (MalformedJwtException | UnsupportedJwtException e) {
            log.warn("Malformed or unsupported JWT: {}", e.getMessage());
            throw new BusinessException(ErrorCode.ERR_AUTH_003);
        } catch (IllegalArgumentException e) {
            log.warn("JWT claims string is empty or null: {}", e.getMessage());
            throw new BusinessException(ErrorCode.ERR_AUTH_003);
        }
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .requireIssuer(jwtIssuer)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}