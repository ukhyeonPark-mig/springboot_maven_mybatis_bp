DROP TABLE IF EXISTS board;

CREATE TABLE board (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    title      VARCHAR(200)  NOT NULL,
    content    TEXT          NOT NULL,
    author     VARCHAR(50)   NOT NULL,
    view_count INT           DEFAULT 0,
    created_at TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP     DEFAULT CURRENT_TIMESTAMP
);

INSERT INTO board (title, content, author) VALUES
('Spring Boot 게시판 테스트', '첫 번째 게시글입니다.', '관리자'),
('MyBatis 연동 확인', 'MyBatis가 정상 동작하는지 확인합니다.', '홍길동'),
('Bootstrap 5.3 적용', 'Bootstrap 5.3으로 UI를 구성했습니다.', '김철수');
