CREATE DATABASE IF NOT EXISTS enterprise_db
CHARACTER SET utf8mb4
COLLATE utf8mb4_unicode_ci;





CREATE TABLE IF NOT EXISTS users (
                                     id            BIGINT          NOT NULL AUTO_INCREMENT,
                                     username      VARCHAR(50)     NOT NULL,
    email         VARCHAR(100)    NOT NULL,
    PASSWORD      VARCHAR(255)    NOT NULL,
    full_name     VARCHAR(100)    NULL,
    active        BOOLEAN         NOT NULL DEFAULT TRUE,
    PRIMARY KEY (id),
    UNIQUE KEY uk_users_username (username),
    UNIQUE KEY uk_users_email    (email)
    );

-- Roles table
CREATE TABLE IF NOT EXISTS roles (
                                     id    BIGINT      NOT NULL AUTO_INCREMENT,
                                     NAME  VARCHAR(50) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_roles_name (NAME)
    );

-- Many-to-many join table
CREATE TABLE IF NOT EXISTS user_roles (
                                          user_id  BIGINT NOT NULL,
                                          role_id  BIGINT NOT NULL,
                                          PRIMARY KEY (user_id, role_id),
    CONSTRAINT fk_user_roles_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_user_roles_role FOREIGN KEY (role_id) REFERENCES roles  (id) ON DELETE CASCADE
    );

-- Seed roles — these must exist before any user can register
INSERT IGNORE INTO roles (NAME) VALUES ('ROLE_USER');
INSERT IGNORE INTO roles (NAME) VALUES ('ROLE_ADMIN');


-- ===============================================================================================================
-- Date June 11, 2026
-- ===============================================================================================================

    -- ----------------------------------------------------------------
    -- Feature 1: API Request/Response Logs
    -- ----------------------------------------------------------------
    CREATE TABLE IF NOT EXISTS api_logs (
        id               BIGINT        NOT NULL AUTO_INCREMENT,
        trace_id         VARCHAR(36)   NULL,
        username         VARCHAR(100)  NULL,
        http_method      VARCHAR(10)   NOT NULL,
        endpoint         VARCHAR(500)  NOT NULL,
        controller_class VARCHAR(255)  NULL,
        method_name      VARCHAR(255)  NULL,
        request_body     MEDIUMTEXT    NULL,
        response_body    MEDIUMTEXT    NULL,
        http_status      INT           NULL,
        execution_ms     BIGINT        NULL,
        client_ip        VARCHAR(50)   NULL,
        error_message    TEXT          NULL,
        created_at       DATETIME(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
        PRIMARY KEY (id),
        INDEX idx_api_logs_trace_id   (trace_id),
        INDEX idx_api_logs_username   (username),
        INDEX idx_api_logs_created_at (created_at),
        INDEX idx_api_logs_endpoint   (endpoint(255))
        );

    -- ----------------------------------------------------------------
    -- Feature 2: Third-Party Integration Logs
    -- ----------------------------------------------------------------
    CREATE TABLE IF NOT EXISTS integration_logs (
        id                BIGINT        NOT NULL AUTO_INCREMENT,
        trace_id          VARCHAR(36)   NULL,
        integration_name  VARCHAR(100)  NOT NULL,
        operation         VARCHAR(100)  NOT NULL,
        http_method       VARCHAR(10)   NULL,
        target_url        VARCHAR(1000) NULL,
        request_payload   MEDIUMTEXT    NULL,
        response_payload  MEDIUMTEXT    NULL,
        http_status       INT           NULL,
        execution_ms      BIGINT        NULL,
        success           BOOLEAN       NOT NULL DEFAULT FALSE,
        error_message     TEXT          NULL,
        retry_count       INT           NOT NULL DEFAULT 0,
        triggered_by      VARCHAR(100)  NULL,
        created_at        DATETIME(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
        PRIMARY KEY (id),
        INDEX idx_intg_logs_trace_id         (trace_id),
        INDEX idx_intg_logs_integration_name (integration_name),
        INDEX idx_intg_logs_created_at       (created_at),
        INDEX idx_intg_logs_success          (success)
        );

    -- ----------------------------------------------------------------
    -- Feature 3: Dynamic Permission Management
    -- ----------------------------------------------------------------
    -- Add 'description' to roles if not already present
    ALTER TABLE roles ADD COLUMN IF NOT EXISTS description VARCHAR(255) NULL;

    CREATE TABLE IF NOT EXISTS permissions (
        id           BIGINT        NOT NULL AUTO_INCREMENT,
        name         VARCHAR(100)  NOT NULL,
        description  VARCHAR(255)  NULL,
        resource     VARCHAR(100)  NOT NULL,   -- e.g. "PRODUCT", "USER", "ORDER"
        action       VARCHAR(50)   NOT NULL,   -- e.g. "READ", "CREATE", "UPDATE", "DELETE"
        active       BOOLEAN       NOT NULL DEFAULT TRUE,
        created_at   DATETIME(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
        PRIMARY KEY (id),
        UNIQUE KEY uk_permissions_name (name),
        INDEX idx_permissions_resource (resource),
        INDEX idx_permissions_action   (action)
        );

    CREATE TABLE IF NOT EXISTS role_permissions (
        role_id        BIGINT NOT NULL,
        permission_id  BIGINT NOT NULL,
        PRIMARY KEY (role_id, permission_id),
        CONSTRAINT fk_rp_role       FOREIGN KEY (role_id)       REFERENCES roles       (id) ON DELETE CASCADE,
        CONSTRAINT fk_rp_permission FOREIGN KEY (permission_id) REFERENCES permissions (id) ON DELETE CASCADE
        );

    -- ----------------------------------------------------------------
    -- Seed: Permissions
    -- --------------------------------------------------------------
    INSERT IGNORE INTO permissions (name, description, resource, action) VALUES
        ('PRODUCT_READ',   'View products',            'PRODUCT', 'READ'),
        ('PRODUCT_CREATE', 'Create new products',      'PRODUCT', 'CREATE'),
        ('PRODUCT_UPDATE', 'Update existing products', 'PRODUCT', 'UPDATE'),
        ('PRODUCT_DELETE', 'Soft-delete products',     'PRODUCT', 'DELETE'),
        ('USER_READ',      'View user profiles',       'USER',    'READ'),
        ('USER_MANAGE',    'Manage user accounts',     'USER',    'MANAGE'),
        ('ROLE_MANAGE',    'Manage roles',             'ROLE',    'MANAGE');

    -- Seed: Assign all permissions to ROLE_ADMIN
    INSERT IGNORE INTO role_permissions (role_id, permission_id)
    SELECT r.id, p.id FROM roles r, permissions p WHERE r.name = 'ROLE_ADMIN';

    -- Seed: Assign read-only permissions to ROLE_USER
    INSERT IGNORE INTO role_permissions (role_id, permission_id)
    SELECT r.id, p.id FROM roles r
                               JOIN permissions p ON p.name IN ('PRODUCT_READ', 'USER_READ')
    WHERE r.name = 'ROLE_USER';


-- ===============================================================================================================
-- Date
-- ===============================================================================================================