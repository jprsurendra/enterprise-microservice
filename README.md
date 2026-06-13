# Enterprise Microservice

> **Production-ready Spring Boot enterprise foundation** — JWT authentication, dynamic RBAC,
> AOP logging, universal HTTP integration gateway, request tracing, async API audit logging,
> and global exception handling. Built as the base platform for **Raj Sahay** (Government
> Fintech LSP for Rajasthan MSME vendors).

---

## Table of Contents

- [Technology Stack](#technology-stack)
- [Project Structure](#project-structure)
- [Architecture & Request Flow](#architecture--request-flow)
- [Feature Overview](#feature-overview)
- [Database Schema](#database-schema)
- [Setup Instructions](#setup-instructions)
- [Environment Variables](#environment-variables)
- [API Reference](#api-reference)
- [Error Codes](#error-codes)
- [Logging Strategy](#logging-strategy)
- [Security Design](#security-design)
- [Architectural Rules](#architectural-rules)
- [Postman Testing](#postman-testing)
- [Troubleshooting](#troubleshooting)
- [Version History](#version-history)

---

## Technology Stack

| Technology | Version | Purpose |
|------------|---------|---------|
| Java | 21 | Programming language (Virtual Threads ready) |
| Spring Boot | 3.4.1 | Core framework |
| Spring Security | 6.x | Authentication & authorization (stateless) |
| Spring Data JPA | 3.4.1 | ORM / database access |
| Spring AOP | 3.4.1 | Profile-based method logging |
| JJWT | 0.12.6 | JWT token generation & validation |
| MySQL | 8.x | Primary database |
| Resilience4j | 2.2.0 | Retry & circuit breaker (IntegrationGateway) |
| SpringDoc OpenAPI | 2.7.0 | Swagger UI & API documentation |
| Apache HttpClient | 5.x | HTTP backend for RestClient |
| spring-dotenv | 4.0.0 | `.env` file support for local dev |
| Lombok | Latest | Boilerplate reduction |
| Maven | 3.9+ | Build tool |

**Profiles:** `dev` | `uat` | `prod`

---

## Project Structure

```
enterprise-microservice/
├── src/main/java/com/enterprise/microservice/
│   ├── EnterpriseMicroserviceApplication.java
│   ├── annotation/
│   │   ├── ApiLog.java                    # Marks controller methods for DB audit logging
│   │   └── CheckPermission.java           # Marks methods requiring a specific permission
│   ├── aspect/
│   │   ├── ApiLogAspect.java              # Intercepts @ApiLog — persists to api_logs table
│   │   ├── DetailedLoggingAspect.java     # dev/uat: full input/output/timing per method
│   │   ├── ProductionLoggingAspect.java   # prod: silent <500ms, warn 500-2000ms, error >2000ms
│   │   └── DynamicPermissionAspect.java   # Intercepts @CheckPermission — queries DB for RBAC
│   ├── config/
│   │   ├── SecurityConfig.java            # Spring Security DSL — stateless, JWT, public paths
│   │   ├── OpenApiConfig.java             # Swagger/OpenAPI — JWT Bearer scheme, server URL
│   │   ├── AsyncConfig.java               # Thread pools: apiLogExecutor, integrationExecutor
│   │   └── RestClientConfig.java          # RestClient bean with Apache HttpClient 5 backend
│   ├── controller/
│   │   ├── AuthController.java            # POST /api/auth/register, POST /api/auth/login
│   │   ├── ApiLogController.java          # GET /api/v1/admin/api-logs/** (ADMIN only)
│   │   ├── RoleManagementController.java  # /api/v1/admin/roles/** (ADMIN only)
│   │   └── TestController.java            # Diagnostic endpoints (ping, admin info)
│   ├── dto/
│   │   ├── LoginRequest.java
│   │   ├── RegisterRequest.java
│   │   ├── JwtResponse.java
│   │   ├── UserResponse.java              # Never contains password field
│   │   ├── ApiResponse.java
│   │   ├── ApiErrorResponse.java          # Standardized error envelope
│   │   ├── CreateRoleRequest.java
│   │   ├── CreatePermissionRequest.java
│   │   ├── RoleResponse.java              # DTO — never exposes JPA entity directly
│   │   ├── PermissionResponse.java        # DTO — never exposes JPA entity directly
│   │   ├── IntegrationRequest.java        # Input for IntegrationGateway.call()
│   │   └── IntegrationResponse.java       # Output from IntegrationGateway.call()
│   ├── entity/
│   │   ├── User.java
│   │   ├── Role.java
│   │   ├── Permission.java
│   │   ├── ApiLogEntity.java              # Every HTTP request logged here
│   │   └── IntegrationLogEntity.java      # Every third-party call logged here
│   ├── exception/
│   │   ├── ErrorCode.java                 # Enum of all error codes (ERR_AUTH_*, ERR_DATA_*)
│   │   ├── BusinessException.java         # Runtime exception with ErrorCode
│   │   └── GlobalExceptionHandler.java    # @RestControllerAdvice — all exceptions → structured JSON
│   ├── filter/
│   │   └── RequestTracingFilter.java      # @Order(1) — traceId, IP, body capture, async DB log
│   ├── health/
│   │   └── DatabaseHealthIndicator.java   # SELECT 1 ping — not count(*). Detail: ADMIN only
│   ├── integration/
│   │   └── IntegrationGateway.java        # Universal HTTP wrapper for ALL third-party calls
│   ├── repository/
│   │   ├── UserRepository.java
│   │   ├── RoleRepository.java
│   │   ├── PermissionRepository.java
│   │   ├── ApiLogRepository.java
│   │   └── IntegrationLogRepository.java
│   ├── security/
│   │   ├── JwtTokenProvider.java          # Token generation, validation, claims extraction
│   │   ├── JwtAuthenticationFilter.java   # Per-request Bearer token validation
│   │   ├── JwtAuthenticationEntryPoint.java # 401 structured JSON (not Spring's default 403)
│   │   ├── CustomUserDetails.java
│   │   └── CustomUserDetailsService.java  # JOIN FETCH — avoids N+1 on role load
│   ├── service/
│   │   ├── UserService.java               # register, login business logic
│   │   └── RoleManagementService.java     # CRUD for roles, permissions, user-role assignment
│   └── util/
│       └── MdcUtil.java
├── src/main/resources/
│   └── application.yml                    # Base + dev/uat/prod profile blocks
├── .env                                   # Local secrets (never committed to git)
├── pom.xml
└── README.md
```

---

## Architecture & Request Flow

```
┌──────────────────────────────────────────────────────────────────────┐
│                          HTTP Request                                │
└──────────────────────────────────────────────────────────────────────┘
                                  │
                                  ▼
┌──────────────────────────────────────────────────────────────────────┐
│  1. RequestTracingFilter  [@Order(1) — runs FIRST on every request]  │
│     • Accepts incoming X-Trace-Id or generates UUID traceId          │
│     • Extracts real client IP (X-Forwarded-For → X-Real-IP → remote)│
│     • Wraps request/response in ContentCaching wrappers              │
│     • Sets X-Trace-Id on response header                             │
│     • After chain: captures request+response body, persists to       │
│       api_logs table ASYNC via apiLogExecutor (zero latency impact)  │
│     • MDC.clear() in finally — critical for thread pool reuse        │
└──────────────────────────────────────────────────────────────────────┘
                                  │
                                  ▼
┌──────────────────────────────────────────────────────────────────────┐
│  2. JwtAuthenticationFilter  [Spring Security filter chain]          │
│     • Extracts Bearer token from Authorization header                │
│     • Validates JWT (signature, expiry, issuer)                      │
│     • Loads user via CustomUserDetailsService (JOIN FETCH — no N+1)  │
│     • Sets Authentication in SecurityContext                         │
│     • On failure: clears context → JwtAuthenticationEntryPoint fires │
└──────────────────────────────────────────────────────────────────────┘
                                  │
                                  ▼
┌──────────────────────────────────────────────────────────────────────┐
│  3. SecurityConfig  [PUBLIC_PATHS check]                             │
│     • Public: /api/auth/**, /api/v1/ping, /swagger-ui/**,            │
│               /v3/api-docs/**, /actuator/health/**                   │
│     • ADMIN only: /actuator/** (except health)                       │
│     • All others: valid JWT required                                 │
└──────────────────────────────────────────────────────────────────────┘
                                  │
                                  ▼
┌──────────────────────────────────────────────────────────────────────┐
│  4. Controller Layer                                                 │
│     • Input validated with @Valid                                    │
│     • Always uses DTOs — never exposes JPA entities                  │
│     • @CheckPermission on methods requiring fine-grained access      │
└──────────────────────────────────────────────────────────────────────┘
                                  │
                                  ▼
┌──────────────────────────────────────────────────────────────────────┐
│  5. DynamicPermissionAspect  [@CheckPermission intercept]            │
│     • Extracts ROLE_ authorities from SecurityContext                │
│     • Queries permission table for role's granted permissions        │
│     • Throws ERR_AUTH_004 if required permission not found           │
│     • TODO: @Cacheable(60s TTL) to avoid DB hit per request          │
└──────────────────────────────────────────────────────────────────────┘
                                  │
                                  ▼
┌──────────────────────────────────────────────────────────────────────┐
│  6. AOP Logging Aspect  [profile-based — @Service pointcut only]     │
│     • dev/uat: DetailedLoggingAspect — full args, result, timing     │
│     • prod: ProductionLoggingAspect — silent / warn / error by ms    │
└──────────────────────────────────────────────────────────────────────┘
                                  │
                                  ▼
┌──────────────────────────────────────────────────────────────────────┐
│  7. Service Layer  [@Transactional owner]                            │
│     • All business logic lives here                                  │
│     • Read operations: @Transactional(readOnly=true)                 │
│     • Throws BusinessException(ErrorCode) on business rule violation │
└──────────────────────────────────────────────────────────────────────┘
                                  │
                                  ▼
┌──────────────────────────────────────────────────────────────────────┐
│  8. Repository Layer  [Spring Data JPA]                              │
│     • Custom @Query with JOIN FETCH to prevent N+1                   │
│     • No @Transactional — owned by service layer                     │
└──────────────────────────────────────────────────────────────────────┘
                                  │
                                  ▼
┌──────────────────────────────────────────────────────────────────────┐
│  MySQL 8 Database                                                    │
│  Tables: users, roles, permissions, user_roles, role_permissions,    │
│          api_logs, integration_logs                                  │
└──────────────────────────────────────────────────────────────────────┘

  ─ ─ ─ ─ On any exception ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─
┌──────────────────────────────────────────────────────────────────────┐
│  GlobalExceptionHandler  [@RestControllerAdvice]                     │
│     • All exceptions → structured ApiErrorResponse JSON              │
│     • Never leaks stack traces, user existence, or PII               │
│     • traceId always included from MDC                               │
└──────────────────────────────────────────────────────────────────────┘

  ─ ─ ─ For third-party calls (OCEN, iFMS, SHPP, etc.) ─ ─ ─ ─ ─ ─ ─
┌──────────────────────────────────────────────────────────────────────┐
│  IntegrationGateway  [com.enterprise.microservice.integration]       │
│     • ALL external HTTP calls go through this single component       │
│     • Per-call timeout, retry with exponential backoff               │
│     • 4xx → not retried;  5xx/timeout → retried up to maxRetries     │
│     • Forwards X-Trace-Id to downstream systems                      │
│     • Async DB log to integration_logs via integrationExecutor       │
└──────────────────────────────────────────────────────────────────────┘
```

---

## Feature Overview

### Feature 1 — JWT Authentication (Stateless)

- `POST /api/auth/register` — creates user, assigns `ROLE_USER`, returns `UserResponse` (no password)
- `POST /api/auth/login` — returns `JwtResponse` with `accessToken`, `tokenType: Bearer`, `expiresIn` (seconds)
- Token carries: `sub` (userId), `username`, `email`, `roles[]`, `iss`, `iat`, `exp`
- Issuer claim set and verified — prevents cross-service token reuse
- Secret validated at startup (≥ 32 bytes) — app fails fast with weak secret
- Password stored as BCrypt (strength=12) — plain text never persisted

### Feature 2 — Dynamic Role-Based Access Control

```
Database-driven RBAC — roles and permissions managed at runtime via API.
No code change or restart needed to add/revoke permissions.

Hierarchy:  User → Roles → Permissions
Convention: Permission name = {RESOURCE}_{ACTION} (e.g. LOAN_READ, ROLE_MANAGE)
```

- `@CheckPermission("PERMISSION_NAME")` annotation on any controller or service method
- `DynamicPermissionAspect` queries DB and checks at runtime
- `RoleManagementController` — full CRUD for roles, permissions, and user-role assignment

### Feature 3 — Universal API Logging (Auto — No Annotation Needed)

Every HTTP request is automatically captured in `api_logs` table by `RequestTracingFilter`:

| Field | Source |
|-------|--------|
| `trace_id` | MDC / UUID generated per request |
| `username` | SecurityContext after JWT validation |
| `http_method` | HttpServletRequest |
| `endpoint` | HttpServletRequest URI |
| `request_body` | ContentCachingRequestWrapper (masked for `/api/auth/login`) |
| `response_body` | ContentCachingResponseWrapper (masked for `/api/auth/login`) |
| `http_status` | HttpServletResponse status |
| `execution_ms` | System.currentTimeMillis() delta |
| `client_ip` | X-Forwarded-For → X-Real-IP → remoteAddr |

**Automatically excluded from DB logging** (still appear in app log):
`/actuator/health`, `/api/v1/ping`, `/favicon.ico`

**Sensitive paths** — body captured as `[MASKED]`:
`/api/auth/login` (request body contains password; response body contains JWT token)

### Feature 4 — Third-Party Integration Gateway

All external system calls (OCEN, iFMS, SHPP, RajSign, GSTN, etc.) go through `IntegrationGateway`:

```java
IntegrationResponse response = integrationGateway.call(
    IntegrationRequest.builder()
        .integrationName("OCEN")
        .operation("LOAN_PRODUCT_REGISTER")
        .httpMethod("POST")
        .url(ocenConfig.getProductRegisterUrl())
        .body(payload)
        .timeoutSeconds(30)
        .maxRetries(2)
        .logRequestBody(true)
        .logResponseBody(true)
        .build()
);
```

- Every call logged to `integration_logs` table asynchronously
- Exponential backoff: 1s → 2s → 4s (capped 10s)
- `4xx` responses → not retried (client error, no point retrying)
- `5xx` / timeout → retried up to `maxRetries`
- Always returns `IntegrationResponse` — callers never see raw exceptions
- `X-Trace-Id` forwarded automatically to downstream system

### Feature 5 — Request Tracing

- Every request gets a UUID `traceId` injected into SLF4J MDC
- Appears in every log line automatically via log pattern `[traceId=%X{traceId}]`
- `X-Trace-Id` echoed in response header — client-side correlation
- Accepts upstream `X-Trace-Id` header — end-to-end trace from iFMS/SHPP → RajSahay → OCEN → Bank

### Feature 6 — Profile-Based AOP Logging

| Profile | Aspect | Behaviour |
|---------|--------|-----------|
| `dev`, `uat` | `DetailedLoggingAspect` | Logs all method args, return values, execution time |
| `prod` | `ProductionLoggingAspect` | Silent <500ms · WARN 500–2000ms · ERROR >2000ms |

Pointcut: `@within(Service)` — never `@RestController` (avoids double logging).

### Feature 7 — Global Exception Handling

| Exception | HTTP Status | ErrorCode |
|-----------|-------------|-----------|
| `AccessDeniedException` | 403 | ERR_AUTH_004 |
| `MethodArgumentNotValidException` | 400 | ERR_DATA_002 |
| `HttpMessageNotReadableException` | 400 | ERR_DATA_002 |
| `NoResourceFoundException` | 404 | ERR_DATA_001 |
| `HttpRequestMethodNotSupportedException` | 405 | ERR_SYS_001 |
| `BusinessException` | 422 | (from exception) |
| `Exception` (fallback) | 500 | ERR_SYS_001 |

All errors return:
```json
{
  "timestamp": "2026-06-12T14:22:41",
  "statusCode": 401,
  "errorCode": "ERR_AUTH_003",
  "message": "Authentication required. Provide a valid Bearer token.",
  "path": "/api/v1/admin/roles",
  "traceId": "a1b2c3d4-..."
}
```

### Feature 8 — Async Thread Pools

| Pool | Core | Max | Queue | Used For |
|------|------|-----|-------|----------|
| `apiLogExecutor` | 2 | 10 | 500 | api_logs persistence |
| `integrationExecutor` | 3 | 15 | 200 | integration_logs persistence |

Both pools: `waitForTasksToCompleteOnShutdown=true` — no log loss on graceful shutdown.

---

## Database Schema

```sql
-- Core identity tables
CREATE TABLE users (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    username   VARCHAR(50)  UNIQUE NOT NULL,
    email      VARCHAR(255) UNIQUE NOT NULL,
    password   VARCHAR(255) NOT NULL,            -- BCrypt(12)
    full_name  VARCHAR(255),
    active     BOOLEAN DEFAULT TRUE,
    created_at DATETIME,
    updated_at DATETIME
);

CREATE TABLE roles (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    name        VARCHAR(50) UNIQUE NOT NULL,      -- e.g. ROLE_ADMIN
    description VARCHAR(255),
    created_at  DATETIME
);

CREATE TABLE permissions (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    name        VARCHAR(100) UNIQUE NOT NULL,     -- e.g. LOAN_READ
    description VARCHAR(255),
    resource    VARCHAR(100),                     -- e.g. LOAN
    action      VARCHAR(50),                      -- READ|CREATE|UPDATE|DELETE|MANAGE
    active      BOOLEAN DEFAULT TRUE,
    created_at  DATETIME
);

-- Join tables
CREATE TABLE user_roles       (user_id BIGINT, role_id BIGINT);
CREATE TABLE role_permissions (role_id BIGINT, permission_id BIGINT);

-- Audit tables (append-only in production)
CREATE TABLE api_logs (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    trace_id      VARCHAR(36),
    username      VARCHAR(100),
    http_method   VARCHAR(10) NOT NULL,
    endpoint      VARCHAR(500) NOT NULL,
    controller_class VARCHAR(255),
    method_name   VARCHAR(255),
    request_body  MEDIUMTEXT,
    response_body MEDIUMTEXT,
    http_status   INT,
    execution_ms  BIGINT,
    client_ip     VARCHAR(50),
    error_message TEXT,
    created_at    DATETIME
);

CREATE TABLE integration_logs (
    id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    trace_id         VARCHAR(36),
    integration_name VARCHAR(100),
    operation        VARCHAR(100),
    http_method      VARCHAR(10),
    target_url       VARCHAR(2000),
    request_payload  MEDIUMTEXT,
    response_payload MEDIUMTEXT,
    http_status      INT,
    execution_ms     BIGINT,
    success          BOOLEAN,
    error_message    TEXT,
    retry_count      INT,
    triggered_by     VARCHAR(100),
    created_at       DATETIME
);
```

**Seeded data (auto-created on `dev` profile):**

| Roles | Permissions |
|-------|-------------|
| `ROLE_ADMIN` | `PRODUCT_READ`, `PRODUCT_CREATE`, `PRODUCT_UPDATE`, `PRODUCT_DELETE`, `USER_READ`, `USER_MANAGE`, `ROLE_MANAGE` |
| `ROLE_USER` | `PRODUCT_READ`, `USER_READ` |

---

## Setup Instructions

### Prerequisites

- Java 21+
- MySQL 8.x running locally
- Maven 3.9+

### Step 1 — Create Database

```sql
CREATE DATABASE enterprise_db
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;
```

### Step 2 — Seed Admin User

```sql
-- Password: Admin@123
INSERT INTO users (username, email, password, full_name, active, created_at, updated_at)
VALUES (
  'admin',
  'admin@enterprise.com',
  '$2a$12$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2uheWG/igi.',
  'System Administrator', true, NOW(), NOW()
);

-- Assign ROLE_ADMIN (run after app starts and seeds roles)
INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id FROM users u, roles r
WHERE u.username = 'admin' AND r.name = 'ROLE_ADMIN';
```

### Step 3 — Create `.env` File

Create `.env` in the project root (never commit this file):

```properties
SPRING_PROFILES_ACTIVE=dev

# Database
DB_URL=jdbc:mysql://localhost:3306/enterprise_db?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true
DB_USERNAME=your_mysql_username
DB_PASSWORD=your_mysql_password

# JWT — minimum 32 characters, no default provided
JWT_SECRET=your_super_secret_jwt_key_minimum_32_characters_long
JWT_EXPIRATION_MS=3600000

# Server
SERVER_PORT=8080
```

### Step 4 — Build & Run

```bash
# Clean build
mvn clean compile

# Run with dev profile
mvn spring-boot:run

# Or run JAR directly
mvn clean package -DskipTests
java -jar target/enterprise-microservice-1.0.0.jar
```

### Step 5 — Verify

```bash
# Health check (public)
curl http://localhost:8080/actuator/health

# Ping (public)
curl http://localhost:8080/api/v1/ping

# Swagger UI
open http://localhost:8080/swagger-ui.html
```

---

## Environment Variables

| Variable | Required | Description | Example |
|----------|----------|-------------|---------|
| `SPRING_PROFILES_ACTIVE` | Yes | Active profile | `dev` / `uat` / `prod` |
| `DB_URL` | Yes | JDBC connection string | `jdbc:mysql://localhost:3306/enterprise_db` |
| `DB_USERNAME` | Yes | Database username | `root` |
| `DB_PASSWORD` | Yes | Database password | `secret` |
| `JWT_SECRET` | Yes | JWT signing secret (≥32 chars) | `my_super_secret_key_32_chars_min` |
| `JWT_EXPIRATION_MS` | No | Token TTL in milliseconds | `3600000` (1 hour) |
| `SERVER_PORT` | No | HTTP port | `8080` |
| `INTEGRATION_CONNECT_TIMEOUT_MS` | No | HTTP client connect timeout | `5000` |
| `INTEGRATION_READ_TIMEOUT_MS` | No | HTTP client read timeout | `30000` |

> **⚠️ JWT_SECRET has no fallback.** The application will refuse to start if it is missing or shorter than 32 bytes.

---

## API Reference

### Authentication (Public)

| Method | Endpoint | Description |
|--------|----------|-------------|
| `POST` | `/api/auth/register` | Register new user |
| `POST` | `/api/auth/login` | Login — returns JWT |

**Register request:**
```json
{
  "username": "john_doe",
  "email": "john@example.com",
  "password": "Secret@123",
  "fullName": "John Doe"
}
```
Password rules: min 8 chars, requires uppercase + lowercase + digit + special character.

**Login response:**
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
  "tokenType": "Bearer",
  "expiresIn": 3600
}
```
`expiresIn` is in **seconds** (JWT spec compliant).

---

### Role Management (ADMIN only — `Authorization: Bearer <token>`)

| Method | Endpoint | Permission | Description |
|--------|----------|------------|-------------|
| `GET` | `/api/v1/admin/roles` | ROLE_ADMIN | List all roles with permissions |
| `POST` | `/api/v1/admin/roles` | ROLE_MANAGE | Create new role |
| `GET` | `/api/v1/admin/roles/permissions` | ROLE_ADMIN | List all permissions |
| `POST` | `/api/v1/admin/roles/permissions` | ROLE_MANAGE | Create new permission |
| `POST` | `/api/v1/admin/roles/{roleId}/permissions` | ROLE_MANAGE | Assign permissions to role |
| `DELETE` | `/api/v1/admin/roles/{roleId}/permissions` | ROLE_MANAGE | Revoke permissions from role |
| `POST` | `/api/v1/admin/roles/users/{userId}/assign` | ROLE_MANAGE | Assign roles to user |
| `DELETE` | `/api/v1/admin/roles/users/{userId}/revoke` | ROLE_MANAGE | Revoke roles from user |

---

### API Logs (ADMIN only)

| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET` | `/api/v1/admin/api-logs` | All logs (paginated) |
| `GET` | `/api/v1/admin/api-logs/user/{username}` | Logs by username |
| `GET` | `/api/v1/admin/api-logs/range?from=&to=` | Logs by date-time range |
| `GET` | `/api/v1/admin/api-logs/{id}` | Single log entry |

---

### Diagnostics

| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| `GET` | `/api/v1/ping` | Public | Returns `pong` |
| `GET` | `/api/v1/admin/info` | ROLE_ADMIN | Admin status check |
| `GET` | `/actuator/health` | Public / ROLE_ADMIN | Basic UP/DOWN or full DB detail |
| `GET` | `/actuator/metrics` | ROLE_ADMIN | JVM, HTTP metrics |

---

## Error Codes

| Code | HTTP | Meaning |
|------|------|---------|
| `ERR_AUTH_001` | 401 | Invalid credentials |
| `ERR_AUTH_002` | 401 | JWT token expired |
| `ERR_AUTH_003` | 401 | JWT invalid / malformed / tampered |
| `ERR_AUTH_004` | 403 | Access denied — insufficient role or permission |
| `ERR_DATA_001` | 404 | Resource not found |
| `ERR_DATA_002` | 400 | Validation failed |
| `ERR_DATA_003` | 422 | Resource conflict (duplicate) |
| `ERR_SYS_001` | 500 | Unexpected internal error |

---

## Logging Strategy

### Log Pattern

```
%d{ISO8601} [%thread] %-5level %logger{36} [traceId=%X{traceId}] [ip=%X{clientIp}] - %msg%n
```

Every log line includes `traceId` and `clientIp` automatically.

### Profile Behaviour

| Profile | SQL Logging | App Level | AOP Logging |
|---------|------------|-----------|-------------|
| `dev` | `show-sql: true` | DEBUG | Full args/results |
| `uat` | `show-sql: false` | INFO | Full args/results |
| `prod` | `show-sql: false` | WARN | Silent/warn/error by ms |

### What Gets Logged Where

| Layer | Destination | Async? |
|-------|-------------|--------|
| Every HTTP request | `api_logs` DB table | ✅ Yes |
| Every third-party call | `integration_logs` DB table | ✅ Yes |
| Slow/error methods (prod) | Application log file | ❌ Inline |
| Request summary line | Application log file | ❌ Inline |

---

## Security Design

### Decisions Made

| Decision | Rationale |
|----------|-----------|
| Stateless JWT — no sessions | Horizontal scaling without sticky sessions |
| BCrypt strength 12 | Industry standard for password hashing |
| JWT secret ≥ 32 bytes enforced at startup | Fail-fast prevents weak secrets reaching production |
| Issuer claim set and verified | Prevents token reuse across different services |
| Generic auth error messages | Never reveals whether a username exists |
| `@Data` banned on JPA entities | Prevents JPA dirty-checking issues and hash collisions |
| `@Data` banned on `UserDetails` | Prevents password hash leaking in equals/hashCode |
| `JwtAuthenticationEntryPoint` | Returns 401 JSON — not Spring's default 403 |
| Response headers set | X-Frame-Options, HSTS, X-Content-Type-Options, Referrer-Policy |
| `/api/auth/login` body masked in logs | JWT token in response must never be persisted |

### Security Response Headers

```
X-Frame-Options: DENY
Strict-Transport-Security: max-age=31536000; includeSubDomains
X-Content-Type-Options: nosniff
Referrer-Policy: no-referrer
```

---

## Architectural Rules

These 8 rules are enforced across all modules. Never violate them.

1. **Package Consistency** — All DTOs in `.dto`, entities in `.entity`, services in `.service`. `IntegrationGateway` stays in `.integration` (infrastructure, not a business service).

2. **Entity Rules** — Never `@Data` on JPA entities. Always `@Getter @Setter @NoArgsConstructor`. `equals/hashCode` on `id` field only. No `@ToString` with associations.

3. **Transaction Ownership** — `@Transactional` on service layer only. Read operations always use `@Transactional(readOnly = true)`.

4. **Security** — BCrypt strength 12. JWT secret validated at startup. Error messages never reveal user existence or internal details.

5. **Logging** — Never log PII in production. `MDC.clear()` always in finally. AOP pointcut on `@Service` only — never `@RestController`.

6. **DTO Pattern** — Controllers always use DTOs. Never expose JPA entities in API responses. `UserResponse` never contains password.

7. **Async DB Logging** — Logging failures must never affect the main request. All persistence via dedicated thread pools.

8. **Sample Code is Removed** — The `Product` entity, `ProductController`, `ProductService`, `ProductRepository`, and `ProductDto` were temporary scaffolding and have been deleted. Do not re-introduce them.

---

## Postman Testing

A complete Postman collection (`RajSahay_Postman_Collection.json`) is included with:

- 22 happy-path requests
- 23 error/edge-case requests
- Auto-save of JWT token after login
- Built-in test scripts that validate response structure, security headers, and traceId presence

**Import steps:**
1. Open Postman → Import → select the JSON file
2. The collection has `baseUrl = http://localhost:8080` pre-configured as a collection variable
3. Run `01 — Auth → Login ADMIN` first — JWT auto-saves to `{{jwt_token}}`
4. All subsequent requests use `{{jwt_token}}` automatically

---

## Troubleshooting

| Issue | Likely Cause | Solution |
|-------|-------------|----------|
| App fails to start — JWT secret error | `JWT_SECRET` missing or < 32 chars | Set a strong secret in `.env` |
| `401` on public endpoints | Endpoint missing from `PUBLIC_PATHS` | Add to `SecurityConfig.PUBLIC_PATHS` array |
| `traceId: null` in error response | MDC not set before EntryPoint fires | Add null-safe fallback UUID in `JwtAuthenticationEntryPoint` |
| No data in `api_logs` table | `RequestTracingFilter` not saving | Verify `@Component` and `@Order(1)` on filter |
| `LazyInitializationException` | Association loaded outside transaction | Use `JOIN FETCH` or `FetchType.EAGER` for required associations |
| `403` instead of `401` for no token | `JwtAuthenticationEntryPoint` not wired | Verify `.exceptionHandling(e -> e.authenticationEntryPoint(...))` in SecurityConfig |
| Port 8080 in use | Another process on port | Change `SERVER_PORT` in `.env` or kill the process |
| MySQL connection refused | MySQL not running | `sudo systemctl start mysql` |
| `show-sql` in production logs | Profile not set to prod | Set `SPRING_PROFILES_ACTIVE=prod` in `.env` |

---

## Version History

| Version | Date | Changes |
|---------|------|---------|
| 1.0.0 | 2026-06-09 | Initial: JWT auth, RBAC, AOP logging, request tracing, global exception handling, MySQL |
| 1.1.0 | 2026-06-10 | Added: Dynamic RBAC (`@CheckPermission`), `RoleManagementController`, `ApiLogController`, `IntegrationGateway`, async thread pools, `@ApiLog` AOP |
| 1.2.0 | 2026-06-11 | Added: `RoleResponse`/`PermissionResponse` DTOs, `CreateRoleRequest`/`CreatePermissionRequest` moved to `.dto`, `@Valid` wired on all request bodies |
| 1.3.0 | 2026-06-12 | Fixed: `IntegrationGateway` moved to `.integration` package, `StopWatch` replaced with `currentTimeMillis()` in retry loop, `executeHttp()` uses `toEntity()` for real HTTP status, dead code removed from `AuthController` |
| 1.4.0 | 2026-06-13 | Enhanced: `RequestTracingFilter` upgraded — now captures request/response body via `ContentCachingWrapper` and auto-persists ALL requests to `api_logs` table asynchronously. `@ApiLog` annotation now optional. Sensitive paths auto-masked. |
