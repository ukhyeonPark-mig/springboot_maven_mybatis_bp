-- users (PRD §5.1) — role-based accounts with OTP password-reset state
CREATE TABLE users (
    id                BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    role              VARCHAR(20)  NOT NULL DEFAULT 'client',   -- client | admin
    email             VARCHAR(255) NOT NULL,
    email_verified_at DATETIME     NULL,
    name              VARCHAR(255) NULL,
    password          VARCHAR(255) NOT NULL,                    -- BCrypt hash
    otp               VARCHAR(255) NULL,                        -- password-reset OTP
    otp_expires_at    DATETIME     NULL,
    otp_attempts      INT          NOT NULL DEFAULT 0,
    profile_image     VARCHAR(255) NULL,                        -- R2 filename
    remember_token    VARCHAR(100) NULL,
    created_at        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_users_email (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
