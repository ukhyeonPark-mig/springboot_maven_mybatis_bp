-- 초기 시드 데이터 (PRD §5.4): 기본 관리자 계정 + settings 싱글턴.
-- 기존 Java ApplicationRunner 시더를 대체한다. NOT EXISTS로 보호되어 있어
-- 예전 DataInitializer로 이미 시드된 데이터베이스에서도 안전하게 실행된다.
--
-- ⚠ 운영 환경에 배포하기 전에 시드된 관리자 비밀번호를 반드시 변경할 것.
-- admin@example.com / password1!  (BCrypt cost 12, PRD §6.1)

INSERT INTO users (role, email, email_verified_at, name, password, otp_attempts)
SELECT 'admin', 'admin@example.com', NOW(), '관리자',
       '$2a$12$keamfpfCARu1Sqa70hl/sOt5BIavX109bEMh7Vwp42Tg148K0c/oe', 0
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM users WHERE email = 'admin@example.com');

INSERT INTO settings (footer, version)
SELECT CONCAT('© ', YEAR(CURDATE()), ' BP'), '1.0.0'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM settings);
