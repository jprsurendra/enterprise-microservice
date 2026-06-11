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