-- V2__create_users.sql
-- Users, refresh tokens, and the user-role join table.

CREATE TABLE users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(100) NOT NULL,
    email VARCHAR(150) NOT NULL,
    password VARCHAR(255) NOT NULL,
    full_name VARCHAR(150),
    active TINYINT(1) NOT NULL DEFAULT 1,
    account_locked TINYINT(1) NOT NULL DEFAULT 0,
    locked_at DATETIME,
    failed_login_attempts INT NOT NULL DEFAULT 0,
    last_login_at DATETIME,
    password_reset_token VARCHAR(255),
    password_reset_token_expiry DATETIME,
    credentials_expired TINYINT(1) NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL,
    updated_at DATETIME,
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    is_deleted TINYINT(1) NOT NULL DEFAULT 0,
    deleted_at DATETIME,
    deleted_by VARCHAR(100),
    CONSTRAINT uq_users_username UNIQUE (username),
    CONSTRAINT uq_users_email UNIQUE (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_users_active ON users(active);

CREATE TABLE user_roles (
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    PRIMARY KEY (user_id, role_id),
    CONSTRAINT fk_user_roles_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_user_roles_role FOREIGN KEY (role_id) REFERENCES roles(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE refresh_tokens (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    token VARCHAR(512) NOT NULL,
    user_id BIGINT NOT NULL,
    expiry_date DATETIME NOT NULL,
    revoked TINYINT(1) NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL,
    CONSTRAINT uq_refresh_tokens_token UNIQUE (token),
    CONSTRAINT fk_refresh_tokens_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_refresh_tokens_user ON refresh_tokens(user_id);

-- Default super admin: username=superadmin / password=Admin@123 (BCrypt hash below)
-- IMPORTANT: change this password immediately after first deployment.
INSERT INTO users (username, email, password, full_name, active, created_at, created_by, is_deleted)
VALUES ('superadmin', 'superadmin@hrms.local', '$2b$10$xbm4yTG7VVaSA38cx.6VdO0hudq3DfaeZxirVz6kmMJt0pzwI2hn.', 'Super Administrator', 1, NOW(), 'SYSTEM', 0);

INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id FROM users u, roles r WHERE u.username = 'superadmin' AND r.name = 'SUPER_ADMIN';
