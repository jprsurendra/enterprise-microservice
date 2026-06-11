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

@Configuration       /* <-- It tells Spring: This is a configuration class. It will be loaded at startup.*/
@EnableWebSecurity   /* <-- Spring Security enables: Without it, there is:  No security, No authentication, No authorization */
@EnableMethodSecurity(prePostEnabled = true)  /* <-- Now you can use method annotation @PreAuthorize("hasRole('ADMIN')") -->  only the ADMIN user can access it.  */
@RequiredArgsConstructor
public class SecurityConfig {
    /*
    Dependency Injection, Spring will inject these objects at startup.
    1. UserDetailsService: UserDetailsService userDetailsService
        Purpose: Retrieving the user from the database.
        At the time of login, Spring checks:
            username = surendra,
            Is it in the database?,
            Is the password correct?,
            What are the roles?
            This is where it is used.
   2. JwtAuthenticationFilter: JwtAuthenticationFilter jwtAuthenticationFilter
        This is the most important security component of the project.
        Request:  GET /api/v1/products
        Authorization: Bearer eyJhbGciOi...
        Filter:
            Reads the token
            Validates it
            Extracts the user
            Stores it in the Security Context
   3. JwtAuthenticationEntryPoint: JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint
        When the token is missing or invalid.
        Example: GET /api/v1/products    Without token
        Response: { "status":401, "message":"Unauthorized" }
        This class sends the response.
     */
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
    // PUBLIC_PATHS is the whitelist.
    private static final String[] PUBLIC_PATHS = {
            // Auth
            "/api/auth/**",  // Means Tokens are not required for /api/auth/register   OR  /api/auth/login  OR  /api/auth/refresh

            // Swagger UI static resources
            "/swagger-ui.html",
            "/swagger-ui/**",

            // SpringDoc OpenAPI spec endpoints (both /api-docs and /v3/api-docs are active)
            "/api-docs",
            "/api-docs/**",
            "/v3/api-docs",
            "/v3/api-docs/**",

            // Actuator liveness/readiness probes — must be reachable by load balancers
            "/actuator/health",       // The Load Balancer and Kubernetes hit this very endpoint.
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