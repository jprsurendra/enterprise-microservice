# Enterprise Microservice

## Project Overview

This is a production-ready enterprise microservice built with **Spring Boot 3.1.5** and **Java 21**, featuring JWT-based authentication, role-based access control (RBAC), AOP logging, request tracing, global exception handling, and MySQL database integration.

## Table of Contents

- [Technology Stack](#technology-stack)
- [Project Structure](#project-structure)
- [File Flow Diagram](#file-flow-diagram)
- [Complete File Descriptions](#complete-file-descriptions)
- [Configuration Files](#configuration-files)
- [File Relationship Summary](#file-relationship-summary)
- [Request Flow Example](#request-flow-example)
- [Setup Instructions](#setup-instructions)
- [Testing the Application](#testing-the-application)
- [Environment Variables](#environment-variables)
- [Error Codes](#error-codes)
- [Actuator Endpoints](#actuator-endpoints)

---

## Technology Stack

| Technology | Version | Purpose |
|------------|---------|---------|
| Java | 21 | Programming language |
| Spring Boot | 3.1.5 | Framework |
| Spring Security | 3.1.5 | Authentication & Authorization |
| Spring Data JPA | 3.1.5 | Database ORM |
| Spring AOP | 3.1.5 | Aspect-oriented logging |
| JJWT | 0.12.3 | JWT token management |
| MySQL | 8.x | Database |
| Maven | 3.9+ | Build tool |
| Lombok | Latest | Boilerplate reduction |

---

## Project Structure

```
enterprise-microservice/
├── src/main/java/com/enterprise/microservice/
│   ├── EnterpriseMicroserviceApplication.java
│   ├── config/
│   │   ├── SecurityConfig.java
│   │   └── AopConfig.java
│   ├── security/
│   │   ├── JwtTokenProvider.java
│   │   ├── JwtAuthenticationFilter.java
│   │   ├── CustomUserDetails.java
│   │   └── CustomUserDetailsService.java
│   ├── aspect/
│   │   ├── DetailedLoggingAspect.java
│   │   └── ProductionLoggingAspect.java
│   ├── filter/
│   │   └── RequestTracingFilter.java
│   ├── controller/
│   │   ├── AuthController.java
│   │   ├── ProductController.java
│   │   └── TestController.java
│   ├── dto/
│   │   ├── LoginRequest.java
│   │   ├── JwtResponse.java
│   │   ├── ApiErrorResponse.java
│   │   └── ApiResponse.java
│   ├── entity/
│   │   └── Product.java
│   ├── repository/
│   │   └── ProductRepository.java
│   ├── service/
│   │   └── ProductService.java
│   ├── exception/
│   │   ├── ErrorCode.java
│   │   ├── BusinessException.java
│   │   └── GlobalExceptionHandler.java
│   ├── health/
│   │   └── DatabaseHealthIndicator.java
│   └── util/
│       └── MdcUtil.java
├── src/main/resources/
│   └── application.yml
├── pom.xml
├── .gitignore
└── README.md
```

---

## File Flow Diagram

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                              HTTP Request                                   │
└─────────────────────────────────────────────────────────────────────────────┘
                                      │
                                      ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│ 1. RequestTracingFilter (filter/)                                           │
│    - Generates Trace ID                                                     │
│    - Extracts Client IP from X-Forwarded-For                                │
│    - Injects into MDC for logging                                           │
└─────────────────────────────────────────────────────────────────────────────┘
                                      │
                                      ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│ 2. JwtAuthenticationFilter (security/)                                      │
│    - Extracts JWT from Authorization header                                 │
│    - Validates token                                                        │
│    - Sets authentication in SecurityContext                                 │
└─────────────────────────────────────────────────────────────────────────────┘
                                      │
                                      ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│ 3. SecurityConfig (config/)                                                 │
│    - Determines which endpoints are public vs protected                     │
│    - Applies @PreAuthorize annotations                                      │
└─────────────────────────────────────────────────────────────────────────────┘
                                      │
                                      ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│ 4. Controller Layer (controller/)                                           │
│    - Receives request                                                       │
│    - Validates input (@Valid)                                               │
│    - Calls Service layer                                                    │
└─────────────────────────────────────────────────────────────────────────────┘
                                      │
                                      ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│ 5. AOP Logging Aspect (aspect/)                                             │
│    - DEV Profile: Logs all inputs/outputs/execution time                   │
│    - PROD Profile: Logs only slow methods (>1s)                            │
└─────────────────────────────────────────────────────────────────────────────┘
                                      │
                                      ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│ 6. Service Layer (service/)                                                 │
│    - Business logic                                                         │
│    - Transaction management (@Transactional)                               │
│    - Throws BusinessException on errors                                    │
└─────────────────────────────────────────────────────────────────────────────┘
                                      │
                                      ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│ 7. Repository Layer (repository/)                                           │
│    - Data access using Spring Data JPA                                     │
│    - Custom queries (@Query)                                               │
└─────────────────────────────────────────────────────────────────────────────┘
                                      │
                                      ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│ MySQL Database                                                              │
│ Table: products                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
                                      │
                                      ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│ On Error: GlobalExceptionHandler (exception/)                               │
│    - Catches all exceptions                                                 │
│    - Returns standardized ApiErrorResponse                                 │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## Complete File Descriptions

### 1. Root Package (`com.enterprise.microservice`)

| File | Description |
|------|-------------|
| `EnterpriseMicroserviceApplication.java` | Main Spring Boot entry point with `@SpringBootApplication` |

### 2. Config Package (`config/`)

| File | Description |
|------|-------------|
| `SecurityConfig.java` | Configures Spring Security: disables CSRF, sets stateless session, defines public endpoints (`/api/auth/**`, `/actuator/**`), configures JWT filter |
| `AopConfig.java` | Enables AspectJ auto-proxy with `@EnableAspectJAutoProxy` |

### 3. Security Package (`security/`)

| File | Description | Relationships |
|------|-------------|---------------|
| `JwtTokenProvider.java` | Core JWT utility: generates tokens, validates, extracts username/claims | Used by: `JwtAuthenticationFilter`, `AuthController` |
| `JwtAuthenticationFilter.java` | Intercepts every request, extracts JWT from `Authorization: Bearer <token>` header, validates, sets `SecurityContext` | Uses: `JwtTokenProvider`, `CustomUserDetailsService` |
| `CustomUserDetails.java` | UserDetails implementation: stores user info for Spring Security | Used by: `CustomUserDetailsService` |
| `CustomUserDetailsService.java` | Loads user by username from in-memory map (demo) | Used by: `SecurityConfig`, `JwtAuthenticationFilter` |

**Demo User Credentials:**

| Username | Password | Roles |
|----------|----------|-------|
| admin | admin123 | ADMIN, USER |
| user | admin123 | USER |

### 4. Aspect Package (`aspect/`) - Profile-Based Logging

| File | Profile | Description |
|------|---------|-------------|
| `DetailedLoggingAspect.java` | `@Profile("dev")` | UAT/Development: Logs method entry/exit with full parameters, results, and execution time |
| `ProductionLoggingAspect.java` | `@Profile("prod")` | Production: Logs only slow methods (>100ms warning, >1s error), no parameter logging |

### 5. Filter Package (`filter/`)

| File | Description |
|------|-------------|
| `RequestTracingFilter.java` | Generates `traceId` (UUID), extracts real client IP from `X-Forwarded-For` header, injects both into SLF4J MDC |

**Client IP Extraction Logic:**
1. Check `X-Forwarded-For` header (takes first IP)
2. Fallback to `X-Real-IP`
3. Fallback to `Proxy-Client-IP`
4. Fallback to `request.getRemoteAddr()`

### 6. Controller Package (`controller/`)

| File | Endpoints | Security |
|------|-----------|----------|
| `AuthController.java` | `POST /api/auth/login` | Public |
| | `GET /api/auth/test` | Public |
| `ProductController.java` | `GET /api/products` | `hasAnyRole('USER', 'ADMIN')` |
| | `GET /api/products/{id}` | `hasAnyRole('USER', 'ADMIN')` |
| | `POST /api/products` | `hasRole('ADMIN')` |
| | `PUT /api/products/{id}` | `hasRole('ADMIN')` |
| | `DELETE /api/products/{id}` | `hasRole('ADMIN')` |
| `TestController.java` | `GET /test-env` | Public |
| | `GET /health-check` | Public |

### 7. DTO Package (`dto/`)

| File | Purpose |
|------|---------|
| `LoginRequest.java` | Login payload with `@NotBlank` validation |
| `JwtResponse.java` | Response containing JWT token, type, expiration |
| `ApiErrorResponse.java` | Standardized error response: timestamp, statusCode, errorCode, message, path, traceId |
| `ApiResponse.java` | Generic success/error wrapper for non-exception responses |

**ApiErrorResponse Example:**
```json
{
  "timestamp": "2024-01-15 10:30:00",
  "statusCode": 404,
  "errorCode": "ERR_DATA_001",
  "message": "Product not found with id: 99",
  "path": "/api/products/99",
  "traceId": "abc123-def456"
}
```

### 8. Entity Package (`entity/`)

| File | Table | Fields |
|------|-------|--------|
| `Product.java` | products | id, name, sku (unique), description, price, quantity, category, active, createdAt, updatedAt |

### 9. Repository Package (`repository/`)

| File | Key Methods |
|------|-------------|
| `ProductRepository.java` | `findBySku()`, `findByCategoryAndActiveTrue()`, `findByPriceBetween()`, custom JPQL query, native SQL query, update query with `@Modifying` |

### 10. Service Package (`service/`)

| File | Methods |
|------|---------|
| `ProductService.java` | `getAllProducts()`, `getProductById()`, `createProduct()`, `updateProduct()`, `deleteProduct()` (soft delete), `getProductsByCategory()` |

### 11. Exception Package (`exception/`)

| File | Description |
|------|-------------|
| `ErrorCode.java` | Enum of standardized error codes: ERR_AUTH_001 to ERR_SYS_001 |
| `BusinessException.java` | Custom runtime exception with errorCode field |
| `GlobalExceptionHandler.java` | `@RestControllerAdvice` handling all exceptions globally |

**Error Code Mapping:**

| Error Code | HTTP Status | When Thrown |
|------------|-------------|-------------|
| ERR_AUTH_001 | 401 | Invalid credentials |
| ERR_AUTH_002 | 401 | JWT expired |
| ERR_AUTH_003 | 401 | JWT invalid |
| ERR_AUTH_004 | 403 | Access denied |
| ERR_DATA_NOT_FOUND | 404 | Product not found |
| ERR_DATA_VALIDATION | 400 | Validation fails / SKU duplicate |
| ERR_SYS_001 | 500 | Unexpected error |

### 12. Health Package (`health/`)

| File | Description |
|------|-------------|
| `DatabaseHealthIndicator.java` | Custom HealthIndicator for Spring Boot Actuator. Checks database connectivity |

### 13. Util Package (`util/`)

| File | Description |
|------|-------------|
| `MdcUtil.java` | Utility class for SLF4J MDC operations |

---

## Configuration Files

### application.yml

Location: `src/main/resources/application.yml`

```yaml
spring:
  application:
    name: enterprise-microservice
  profiles:
    active: dev
  datasource:
    url: jdbc:mysql://localhost:3306/enterprise_db?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true
    username: root
    password: your_mysql_password_here
    driver-class-name: com.mysql.cj.jdbc.Driver
  jpa:
    hibernate:
      ddl-auto: update
    properties:
      hibernate:
        dialect: org.hibernate.dialect.MySQL8Dialect
        format_sql: true
    show-sql: true
  security:
    jwt:
      secret: mySuperSecretKeyForJWTTokenGenerationAndValidation2025!Secure
      expiration-ms: 3600000

server:
  port: 8080

management:
  endpoints:
    web:
      exposure:
        include: health,metrics,info
      base-path: /actuator
  endpoint:
    health:
      show-details: always

logging:
  pattern:
    console: "%d{ISO8601} [%thread] %-5level %logger{36} - %msg - %X{traceId} - %X{clientIp}%n"
  level:
    com.enterprise.microservice: DEBUG
    org.springframework.security: DEBUG
```

### pom.xml - Key Dependencies

| Dependency | Purpose |
|------------|---------|
| spring-boot-starter-web | REST APIs |
| spring-boot-starter-security | Authentication/Authorization |
| spring-boot-starter-data-jpa | Database ORM |
| spring-boot-starter-aop | Method-level logging aspects |
| spring-boot-starter-actuator | Health/metrics endpoints |
| jjwt | JWT generation/validation |
| mysql-connector-j | MySQL driver |
| lombok | Reduces boilerplate code |

---

## File Relationship Summary

```
EnterpriseMicroserviceApplication.java (Main)
          │
          ├──► SecurityConfig.java ──► JwtAuthenticationFilter.java
          │                              │
          │                              └──► JwtTokenProvider.java
          │                              └──► CustomUserDetailsService.java
          │                                      └──► CustomUserDetails.java
          │
          ├──► RequestTracingFilter.java (MDC)
          │
          ├──► DetailedLoggingAspect.java (dev profile)
          ├──► ProductionLoggingAspect.java (prod profile)
          │
          ├──► AuthController.java ──► JwtTokenProvider.java
          │
          ├──► ProductController.java ──► ProductService.java
          │                                    │
          │                                    ├──► ProductRepository.java
          │                                    │         └──► Product.java
          │                                    │
          │                                    └──► BusinessException.java
          │                                              └──► ErrorCode.java
          │
          └──► GlobalExceptionHandler.java ──► ApiErrorResponse.java
```

---

## Request Flow Example: Create Product

```
1. Client sends: POST /api/products
   Header: Authorization: Bearer <JWT>
   Body: {"name":"Laptop","sku":"LAP001","price":999.99}

2. RequestTracingFilter adds traceId and clientIp to MDC

3. JwtAuthenticationFilter validates JWT, extracts user "admin" with role "ADMIN"

4. SecurityConfig checks @PreAuthorize("hasRole('ADMIN')") → GRANTED

5. ProductController.createProduct() receives request

6. DetailedLoggingAspect (dev) logs method entry with arguments

7. ProductService.createProduct():
   - Checks if SKU exists → not found
   - Saves to database via ProductRepository

8. DetailedLoggingAspect logs exit with result and execution time

9. Response returns 201 Created with product JSON

10. On any error: GlobalExceptionHandler returns ApiErrorResponse
```

---

## Setup Instructions

### Prerequisites

- Java 21 (Oracle JDK or OpenJDK)
- MySQL 8.x
- Maven 3.9+ (or use included Maven wrapper)

### Step 1: Clone the Repository

```bash
git clone https://github.com/jprsurendra/enterprise-microservice.git
cd enterprise-microservice
```

### Step 2: Create MySQL Database

```sql
CREATE DATABASE enterprise_db;
```

### Step 3: Update application.yml

Edit `src/main/resources/application.yml` and update:

```yaml
spring:
  datasource:
    username: your_mysql_username
    password: your_mysql_password
  security:
    jwt:
      secret: your_secure_jwt_secret_key_min_32_chars
```

### Step 4: Build and Run

```bash
# Using Maven wrapper
./mvnw clean compile
./mvnw spring-boot:run

# Or using system Maven
mvn clean compile
mvn spring-boot:run
```

### Step 5: Verify Application is Running

```bash
curl http://localhost:8080/health-check
# Response: Application is running!
```

---

## Testing the Application

### 1. Login and Get JWT Token

```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}'
```

Response:
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
  "tokenType": "Bearer",
  "expiresIn": 3600000
}
```

### 2. Get All Products (USER or ADMIN)

```bash
curl -X GET http://localhost:8080/api/products \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

### 3. Create Product (ADMIN only)

```bash
curl -X POST http://localhost:8080/api/products \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Test Laptop",
    "sku": "LAP001",
    "description": "High performance laptop",
    "price": 1299.99,
    "quantity": 50,
    "category": "Electronics"
  }'
```

### 4. Get Product by ID

```bash
curl -X GET http://localhost:8080/api/products/1 \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

### 5. Update Product (ADMIN only)

```bash
curl -X PUT http://localhost:8080/api/products/1 \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Updated Laptop",
    "sku": "LAP001",
    "price": 1199.99,
    "quantity": 45,
    "category": "Electronics"
  }'
```

### 6. Delete Product (ADMIN only - Soft Delete)

```bash
curl -X DELETE http://localhost:8080/api/products/1 \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

### 7. Health Check

```bash
curl http://localhost:8080/actuator/health
```

Response:
```json
{
  "status": "UP",
  "components": {
    "database": {
      "status": "UP",
      "details": {
        "database": "MySQL",
        "productCount": 5,
        "status": "Available"
      }
    },
    "ping": {
      "status": "UP"
    }
  }
}
```

### 8. Test with Regular User (Limited Access)

```bash
# Login as regular user
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"user","password":"admin123"}'

# Try to create product (should fail with 403 Forbidden)
curl -X POST http://localhost:8080/api/products \
  -H "Authorization: Bearer USER_JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"name":"Test","sku":"TEST","price":10,"quantity":1,"category":"Test"}'
```

---

## Environment Variables

For production, use environment variables instead of hardcoding in `application.yml`:

```bash
# Database Configuration
export DB_USERNAME=prod_user
export DB_PASSWORD=secure_password
export DB_URL=jdbc:mysql://localhost:3306/enterprise_db?useSSL=true

# JWT Configuration
export JWT_SECRET=your_256_bit_secret_key
export JWT_EXPIRATION_MS=3600000

# Server Configuration
export SERVER_PORT=8080
export SPRING_PROFILES_ACTIVE=prod
```

---

## Actuator Endpoints

| Endpoint | Description | Default Access |
|----------|-------------|----------------|
| `/actuator/health` | Application health (with custom database indicator) | Public |
| `/actuator/metrics` | Application metrics (memory, CPU, etc.) | Public |
| `/actuator/info` | Application information | Public |
| `/actuator/env` | Environment properties | Public |
| `/actuator/httptrace` | HTTP trace information | Public |

---

## Logging

### Development Profile (`dev`)

- Full method input/output logging
- SQL queries shown
- DEBUG level for application packages
- Trace ID and Client IP in all logs

### Production Profile (`prod`)

- Only slow methods logged (>100ms warning, >1s error)
- No parameter logging
- WARN level for application packages
- Trace ID and Client IP in all logs

### Log Format

```
2024-01-15 10:30:00 [http-nio-8080-exec-1] INFO  c.e.m.controller.ProductController - Request processed - Method: GET, URI: /api/products, Status: 200, Duration: 45 ms, Client IP: 192.168.1.100 - traceId: abc123 - clientIp: 192.168.1.100
```

---

## Troubleshooting

### Common Issues and Solutions

| Issue | Solution |
|-------|----------|
| MySQL Connection Error | Verify MySQL is running: `mysql -u root -p`; Check database exists: `SHOW DATABASES;` |
| JWT Secret Too Short | Use at least 32 characters for JWT secret |
| Access Denied (403) | Verify you're using correct JWT token and user has required role |
| Compilation Errors | Run `./mvnw clean compile` to see specific errors |
| Port 8080 Already in Use | Change `server.port` in `application.yml` or kill process using port 8080 |

---

## Repository

GitHub: https://github.com/jprsurendra/enterprise-microservice.git

---

## License

This project is for enterprise use. All rights reserved.

---

## Version History

| Version | Date       | Changes |
|---------|------------|---------|
| 1.0.0 | 2026-06-09 | Initial release: JWT auth, RBAC, AOP logging, request tracing, global exception handling, MySQL integration |