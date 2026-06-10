# 새 페이지/기능 추가 가이드

이 보일러플레이트에 **새 화면 하나를 추가하는 표준 사이클**입니다. 복붙해서 시작하세요.

> 한 사이클: **(필요 시) 마이그레이션 → 도메인 → 매퍼(XML) → 서비스 → 컨트롤러 → 템플릿 → 보안 규칙 → (공개면) 사이트맵**
> DB가 필요 없는 정적 페이지는 도메인/매퍼/서비스 단계를 건너뜁니다.

---

## 0. 예시 시나리오

> ⚠️ **이 문서의 `Notice`/`notice` 관련 파일은 실제 소스코드에 없습니다.** `Notice.java`, `NoticeMapper`(+XML), `NoticeService`, `NoticeController`, `templates/home/notice.html`, `V5__create_notices.sql` 모두 **존재하지 않는 가상 예시**이며, 아래 단계대로 **여러분이 직접 새로 만드는** 파일입니다. (새 페이지 추가 절차를 보여주기 위한 튜토리얼)

"공지사항(notice) 목록 페이지를 `/notice`(공개)에 추가" — DB 테이블 포함 풀 사이클로 설명합니다.

---

## 1. (DB 필요 시) Flyway 마이그레이션 추가

`src/main/resources/db/migration/`에 **다음 버전 번호**로 새 파일을 만듭니다. **기존 파일은 절대 수정 금지.**

`V5__create_notices.sql`:
```sql
CREATE TABLE notices (
    id         BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    title      VARCHAR(255) NOT NULL,
    body       LONGTEXT     NULL,
    created_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```
앱을 재시작하면 Flyway가 자동 적용합니다.

---

## 2. 도메인 객체 (`domain/`)

ORM이 아니므로 **단순 데이터 객체**입니다. `domain/Notice.java`:
```java
package com.example.bp.domain;

import java.time.LocalDateTime;

public class Notice {
    private Long id;
    private String title;
    private String body;
    private LocalDateTime createdAt;
    // getter/setter (또는 record로 작성해도 됨)
}
```
> `application.yml`의 `mybatis.configuration.map-underscore-to-camel-case: true` 덕분에 `created_at` → `createdAt`로 자동 매핑됩니다.

---

## 3. 매퍼 인터페이스 + XML

**인터페이스** `mapper/NoticeMapper.java`:
```java
package com.example.bp.mapper;

import java.util.List;
import com.example.bp.domain.Notice;

public interface NoticeMapper {       // @Mapper 또는 MyBatisConfig의 @MapperScan으로 등록됨
    List<Notice> findAll();
    Notice findById(Long id);
    void insert(Notice notice);
}
```

**XML** `src/main/resources/mappers/NoticeMapper.xml` — **namespace = 인터페이스 풀네임** (이게 연결 고리):
```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE mapper PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN"
        "https://mybatis.org/dtd/mybatis-3-mapper.dtd">
<mapper namespace="com.example.bp.mapper.NoticeMapper">

    <select id="findAll" resultType="Notice">
        SELECT id, title, body, created_at FROM notices ORDER BY id DESC
    </select>

    <select id="findById" resultType="Notice">
        SELECT id, title, body, created_at FROM notices WHERE id = #{id}
    </select>

    <insert id="insert" parameterType="Notice" useGeneratedKeys="true" keyProperty="id">
        INSERT INTO notices (title, body) VALUES (#{title}, #{body})
    </insert>
</mapper>
```
> `resultType="Notice"`는 `mybatis.type-aliases-package: com.example.bp.domain` 설정 덕에 풀네임 없이 씁니다.

---

## 4. 서비스 (`service/`)

`service/NoticeService.java`:
```java
package com.example.bp.service;

import java.util.List;
import com.example.bp.domain.Notice;
import com.example.bp.mapper.NoticeMapper;
import org.springframework.stereotype.Service;

@Service
public class NoticeService {

    private final NoticeMapper noticeMapper;          // 생성자 주입 (final + 생성자)

    public NoticeService(NoticeMapper noticeMapper) {
        this.noticeMapper = noticeMapper;
    }

    public List<Notice> list() {
        return noticeMapper.findAll();
    }
}
```

---

## 5. 컨트롤러 (`web/`)

공개 페이지이므로 `web/home/`에. **공통 경로는 클래스 레벨 `@RequestMapping`**:
```java
package com.example.bp.web.home;

import com.example.bp.service.NoticeService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/notice")
public class NoticeController {

    private final NoticeService noticeService;

    public NoticeController(NoticeService noticeService) {
        this.noticeService = noticeService;
    }

    @GetMapping                            // → GET /notice
    public String index(Model model) {
        model.addAttribute("notices", noticeService.list());   // 데이터는 Model로 전달
        return "home/notice";                                  // 뷰 이름 (templates/home/notice.html)
    }
}
```
> **반환값 = 뷰 이름**, **데이터 = `Model`**. (`return`에 데이터를 담지 않습니다.)

---

## 6. 템플릿 (`templates/`)

`templates/home/notice.html`을 **새로 만들고**, 레이아웃을 decorate 하여 `content` 슬롯을 채웁니다:
```html
<!doctype html>
<html lang="ko" xmlns:th="http://www.thymeleaf.org"
      xmlns:layout="http://www.ultraq.net.nz/thymeleaf/layout"
      layout:decorate="~{layouts/app}">
<head>
    <th:block layout:fragment="seo">
        <title th:text="'공지사항 — ' + ${appName}">공지사항</title>
    </th:block>
</head>
<body>
<div layout:fragment="content">
    <div class="container-xl py-4">
        <h1 class="mb-3">공지사항</h1>
        <div class="card" th:each="n : ${notices}">
            <div class="card-body">
                <h3 th:text="${n.title}">제목</h3>
                <div th:utext="${n.body}">본문</div>
            </div>
        </div>
    </div>
</div>
</body>
</html>
```
> 레이아웃 종류: 공개/클라이언트=`layouts/app`, 관리자=`layouts/admin`, 로그인/법적=`layouts/auth`.
> fragment/HTMX 작성 표준은 **[02-view-conventions.md](02-view-conventions.md)** 참고.

---

## 7. 보안 규칙 (`config/SecurityConfig.java`)

이 프로젝트는 **기본 차단(fail-safe)** 이라, 공개 페이지는 **명시적으로 permitAll** 해야 합니다.
```java
.requestMatchers("/", "/signin", "/signin/**", "/contact", "/notice", ...).permitAll()
```
- 로그인 사용자 전용이면 → `/notice`를 `/client/**` 아래로 두거나 `.requestMatchers("/notice/**").authenticated()`.
- 관리자 전용이면 → `/admin/**` 아래로 두면 자동으로 `hasRole("ADMIN")`.

---

## 8. (공개 페이지면) 사이트맵 등록

`web/home/SitemapController.java`에 공개 경로를 추가해 `/sitemap.xml`에 노출합니다.

---

## ✅ 체크리스트

- [ ] (DB) `V{다음번호}__*.sql` 추가 (기존 파일 수정 금지)
- [ ] 도메인 객체 (snake_case 컬럼 ↔ camelCase 자동매핑 확인)
- [ ] 매퍼 인터페이스 + **namespace=인터페이스 풀네임**인 XML
- [ ] `@Service` + 생성자 주입
- [ ] `@Controller` + 클래스 `@RequestMapping` + 메서드 매핑, **데이터는 Model**
- [ ] 템플릿: `layout:decorate` + `layout:fragment="content"`
- [ ] **SecurityConfig 인가 규칙** (공개면 permitAll, 아니면 자동 보호)
- [ ] (공개면) 사이트맵
- [ ] `./mvnw test`로 컴파일/렌더 확인

---

## 부분 갱신(HTMX)이 필요하면

폼 제출 등으로 **페이지 일부만** 바꾸려면, 컨트롤러가 전체 페이지 대신 **fragment 이름**을 반환합니다:
```java
@PostMapping("/something")
public String save(..., Model model) {
    // ... 처리 ...
    model.addAttribute("success", "저장되었습니다.");
    return "home/notice :: card";     // "card" 조각만 렌더 → HTMX가 해당 영역 swap
}
```
자세한 패턴(`hx-post`/`hx-target`/`hx-swap`, 토스트, 리다이렉트)은 **[02-view-conventions.md](02-view-conventions.md)** 참고.
