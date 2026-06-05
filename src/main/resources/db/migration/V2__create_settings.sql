-- settings (PRD §5.2) — single-row singleton holding app-wide config
CREATE TABLE settings (
    id         BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    footer     VARCHAR(255) NULL,        -- footer text
    version    VARCHAR(255) NULL,        -- app version
    terms      LONGTEXT     NULL,        -- terms of service (HTML)
    privacy    LONGTEXT     NULL,        -- privacy policy (HTML)
    created_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
