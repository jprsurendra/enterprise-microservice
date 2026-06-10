package com.enterprise.microservice.config;

import com.enterprise.microservice.security.JwtAuthenticationEntryPoint;
import com.enterprise.microservice.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
@RequiredArgsConstructor
public class SecurityConfig {

    private final UserDetailsService userDetailsService;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

    /**
     * Public paths that require no authentication.
     * Swagger/OpenAPI paths are included here for local dev/uat.
     * In production, springdoc.swagger-ui.enabled=false and
     * springdoc.api-docs.enabled=false disable these endpoints entirely,
     * so no security rule is needed there.
     */
    private static final String[] PUBLIC_PATHS = {
            // Auth
            "/api/auth/**",

            // Swagger UI static resources
            "/swagger-ui.html",
            "/swagger-ui/**",

            // SpringDoc OpenAPI spec endpoints (both /api-docs and /v3/api-docs are active)
            "/api-docs",
            "/api-docs/**",
            "/v3/api-docs",
            "/v3/api-docs/**",

            // Actuator liveness/readiness probes — must be reachable by load balancers
            "/actuator/health",
            "/actuator/health/**"
    };

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // ----------------------------------------------------------------
                // 1. Stateless API — disable CSRF (no session cookies) and sessions
                // ----------------------------------------------------------------
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // ----------------------------------------------------------------
                // 2. Hardened security response headers
                // ----------------------------------------------------------------
                .headers(headers -> headers
                        .frameOptions(frame -> frame.deny())
                        .contentTypeOptions(content -> {})
                        .httpStrictTransportSecurity(hsts -> hsts
                                .includeSubDomains(true)
                                .maxAgeInSeconds(31536000))
                        .referrerPolicy(referrer ->
                                referrer.policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.NO_REFERRER)))

                // ----------------------------------------------------------------
                // 3. Exception handling
                //    - authenticationEntryPoint → 401 JSON for missing/invalid JWT
                //      (without this, Spring returns a 403 for unauthenticated requests)
                //    - AccessDeniedException (403) is handled by GlobalExceptionHandler
                // ----------------------------------------------------------------
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(jwtAuthenticationEntryPoint))

                // ----------------------------------------------------------------
                // 4. Endpoint authorization rules
                // ----------------------------------------------------------------
                .authorizeHttpRequests(auth -> auth
                        // All PUBLIC_PATHS above — no token required
                        .requestMatchers(PUBLIC_PATHS).permitAll()

                        // Remaining actuator endpoints (/metrics, /info, /env, etc.)
                        // require ADMIN — only reachable with a valid ROLE_ADMIN JWT
                        .requestMatchers(HttpMethod.GET, "/actuator/**").hasRole("ADMIN")

                        // Every other request must be authenticated
                        .anyRequest().authenticated()
                )

                // ----------------------------------------------------------------
                // 5. Auth provider + JWT filter
                // ----------------------------------------------------------------
                .authenticationProvider(authenticationProvider())
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }
}