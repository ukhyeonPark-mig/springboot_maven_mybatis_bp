-- settings (PRD §5.2) — 애플리케이션 전역 설정을 담는 단일 행 싱글턴
CREATE TABLE settings (
    id         BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    footer     VARCHAR(255) NULL,        -- 푸터 문구
    version    VARCHAR(255) NULL,        -- 애플리케이션 버전
    terms      LONGTEXT     NULL,        -- 서비스 이용약관 (HTML)
    privacy    LONGTEXT     NULL,        -- 개인정보 처리방침 (HTML)
    created_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
