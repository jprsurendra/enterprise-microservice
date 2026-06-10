package com.enterprise.microservice.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * OpenAPI 3.0 metadata and global security scheme configuration.
 *
 * The JWT Bearer scheme defined here wires the "Authorize" button in Swagger UI.
 * Once you paste a token there, every "Try it out" request automatically sends
 * the Authorization: Bearer <token> header.
 */
@Configuration
public class OpenApiConfig {

    private static final String SECURITY_SCHEME_NAME = "bearerAuth";

    @Value("${server.port:8080}")
    private String serverPort;

    @Bean
    public OpenAPI enterpriseOpenAPI() {
        return new OpenAPI()
                .info(apiInfo())
                .servers(List.of(
                        new Server()
                                .url("http://localhost:" + serverPort)
                                .description("Local Development Server")
                ))
                // Global security requirement — applies Bearer auth to every endpoint by default.
                // Individual endpoints can override with @SecurityRequirements({}) to opt out.
                .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME_NAME))
                .components(new Components()
                        .addSecuritySchemes(SECURITY_SCHEME_NAME, jwtSecurityScheme()));
    }

    private Info apiInfo() {
        return new Info()
                .title("Enterprise Microservice API")
                .description("""
                        ## Enterprise Microservice — REST API Reference
                        
                        This API provides product management capabilities secured with JWT-based authentication.
                        
                        ### Authentication Flow
                        1. Call `POST /api/auth/login` with valid credentials.
                        2. Copy the `accessToken` from the response.
                        3. Click the **Authorize** button (🔒) at the top of this page.
                        4. Paste the token in the format: `<your_token>` (the `Bearer ` prefix is added automatically).
                        5. All subsequent **Try it out** calls will include the JWT header.
                        
                        ### Roles
                        | Role | Permissions |
                        |------|-------------|
                        | `ROLE_USER` | Read-only access to products |
                        | `ROLE_ADMIN` | Full CRUD + actuator access |
                        
                        ### Error Codes
                        | Code | Meaning |
                        |------|---------|
                        | ERR_AUTH_001 | Invalid credentials |
                        | ERR_AUTH_002 | Token expired |
                        | ERR_AUTH_003 | Token invalid/malformed |
                        | ERR_AUTH_004 | Access denied |
                        | ERR_DATA_001 | Resource not found |
                        | ERR_DATA_002 | Validation failure |
                        | ERR_DATA_003 | Resource conflict |
                        | ERR_SYS_001 | Internal server error |
                        """)
                .version("1.0.0")
                .contact(new Contact()
                        .name("Enterprise Platform Team")
                        .email("platform@enterprise.com")
                        .url("https://enterprise.com"))
                .license(new License()
                        .name("Proprietary")
                        .url("https://enterprise.com/license"));
    }

    private SecurityScheme jwtSecurityScheme() {
        return new SecurityScheme()
                .name(SECURITY_SCHEME_NAME)
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT")
                .description("Paste your JWT token below. Obtained from POST /api/auth/login.");
    }
}