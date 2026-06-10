-- users (PRD §5.1) — OTP 비밀번호 재설정 상태를 포함한 역할 기반 계정
CREATE TABLE users (
    id                BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    role              VARCHAR(20)  NOT NULL DEFAULT 'client',   -- client(일반) | admin(관리자)
    email             VARCHAR(255) NOT NULL,
    email_verified_at DATETIME     NULL,
    name              VARCHAR(255) NULL,
    password          VARCHAR(255) NOT NULL,                    -- BCrypt 해시
    otp               VARCHAR(255) NULL,                        -- 비밀번호 재설정용 OTP
    otp_expires_at    DATETIME     NULL,
    otp_attempts      INT          NOT NULL DEFAULT 0,
    profile_image     VARCHAR(255) NULL,                        -- R2 파일명
    remember_token    VARCHAR(100) NULL,
    created_at        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_users_email (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
