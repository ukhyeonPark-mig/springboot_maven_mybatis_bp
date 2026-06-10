# 뷰 작성 규약 (Thymeleaf · HTMX · Tabler)

화면을 만들 때 지키는 표준 골격과 HTMX/Tabler 패턴입니다. 새 페이지는 **기존 템플릿을 복사해서** 시작하는 걸 권장합니다.

---

## 1. 페이지 표준 골격 (전체 페이지)

```html
<!doctype html>
<html lang="ko" xmlns:th="http://www.thymeleaf.org"
      xmlns:layout="http://www.ultraq.net.nz/thymeleaf/layout"
      xmlns:sec="http://www.thymeleaf.org/extras/spring-security"
      layout:decorate="~{layouts/app}">          <!-- app / admin / auth 중 택1 -->
<head>
    <th:block layout:fragment="seo">              <!-- 페이지별 title/메타 -->
        <title th:text="'페이지명 — ' + ${appName}">페이지명</title>
    </th:block>
    <th:block layout:fragment="css"></th:block>   <!-- 페이지 전용 CSS가 필요할 때만 -->
</head>
<body>
<div layout:fragment="content">                   <!-- ★ 본문은 여기에 -->
    ...
</div>
<th:block layout:fragment="js"></th:block>        <!-- 페이지 전용 JS가 필요할 때만 -->
</body>
</html>
```

- 레이아웃 선택: **공개/클라이언트=`layouts/app`**, **관리자=`layouts/admin`**, **로그인/법적=`layouts/auth`**, **오류=`layouts/error`**.
- 공통 head/스크립트(Tabler CSS/JS, HTMX)는 레이아웃이 `fragments/commons.html`에서 가져오므로 **페이지에서 다시 넣지 않습니다.**

---

## 2. fragment 정의 & 재사용

**조각 정의** — `th:fragment="이름"`:
```html
<div id="info-card" th:fragment="card">
    ...
</div>
```

**조각 가져오기** — `th:replace="~{파일 :: 이름}"` (호스트 태그를 조각으로 교체):
```html
<th:block th:replace="~{fragments/app_navbar :: navbar}"></th:block>
<th:block th:replace="~{fragments/message :: toasts}"></th:block>
```
- `th:replace` = 호스트 태그 자체를 교체, `th:insert` = 호스트 태그 안에 삽입.
- `th:block`은 출력이 없는 유령 태그 — 조각만 남기고 싶을 때 사용.

---

## 3. HTMX 부분 갱신 패턴

폼/버튼이 AJAX로 요청 → 서버가 **fragment**를 반환 → DOM 일부 교체.

```html
<div id="info-card" th:fragment="card">
    <th:block th:replace="~{fragments/message :: toasts}"></th:block>  <!-- 토스트 포함 -->
    <form hx-post="/admin/setting/information"
          hx-target="#info-card"        <!-- 응답을 넣을 대상 (보통 자기 자신) -->
          hx-swap="outerHTML">          <!-- 교체 방식: 통째로 -->
        <input name="footer" th:value="${footer}">
        <button type="submit" class="btn btn-primary">저장</button>
    </form>
</div>
```
컨트롤러는 `return "admin/setting/information :: card";` 로 그 조각만 렌더합니다.

| 속성 | 의미 |
|---|---|
| `hx-get` / `hx-post` | AJAX 요청 (GET/POST) |
| `hx-target="#id"` | 응답 HTML을 넣을 요소 |
| `hx-swap="outerHTML"` | 교체 방식 (`outerHTML`/`innerHTML`/`beforeend` 등) |
| `hx-trigger="..."` | 트리거 이벤트 (기본: form=submit, button=click) |
| `hx-include="#id"` | 요청에 함께 보낼 추가 입력 |
| `hx-encoding="multipart/form-data"` | 파일 업로드 시 |

### 자주 쓰는 실제 패턴

**삭제 확인** (`hx-confirm`):
```html
<button class="btn btn-sm btn-danger"
        hx-post="/admin/development/backup/delete/{name}"
        hx-confirm="이 백업을 삭제하시겠습니까?"
        hx-target="#backup-card" hx-swap="outerHTML">삭제</button>
```

**로딩 인디케이터** (`hx-indicator` + `htmx-indicator`):
```html
<button id="contactBtn" type="submit" hx-indicator="#contactBtn">
    <span class="label">보내기</span>
    <span class="htmx-indicator">전송 중…</span>   <!-- 요청 중에만 표시 -->
</button>
<style>
    #contactBtn .htmx-indicator { display: none; }
    #contactBtn.htmx-request .htmx-indicator { display: inline-flex; }
    #contactBtn.htmx-request .label { display: none; }
</style>
```

**검색 디바운스** (`hx-trigger` with delay):
```html
<input name="search" hx-get="/admin/user/panel" hx-target="#user-panel"
       hx-trigger="keyup changed delay:500ms, search" hx-include="#filterRole"/>
```

### 서버 → 클라이언트 신호 (응답 헤더)

| 헤더 | 효과 |
|---|---|
| `HX-Redirect: /path` | 전체 페이지 이동 (로그인 성공 등). 본문은 `fragments/empty :: empty` 반환 |
| `HX-Trigger: turnstileReset` | 클라이언트 JS 이벤트 발생 (예: Turnstile 위젯 리셋) |
| `HX-Reswap: none` | 응답으로 타깃을 교체하지 않음 (OOB 토스트만 띄울 때) |

---

## 4. 플래시 메시지(토스트)

`model.addAttribute("success"|"error"|"message", "문구")` 하면, 카드 안에 포함된 `fragments/message :: toasts`가 **우하단 토스트**로 렌더합니다.
- 리다이렉트 시엔 세션 플래시(`FlashMessage.success(session, ...)`)로 **다음 페이지**에서 표시.
- 전역 예외(`GlobalExceptionHandler`)는 OOB 오류 토스트(`fragments/message :: errorOob`)를 사용.

---

## 5. CSRF (신경 쓸 것 없음)

모든 HTMX 요청에 CSRF 토큰이 **자동 첨부**됩니다 — 레이아웃의 meta 태그 + `fragments/htmx_csrf.html` 스크립트가 `htmx:configRequest`에서 헤더를 붙여줍니다. 폼에 `@csrf` 같은 걸 따로 넣지 않아도 됩니다.

---

## 6. 보안/역할 분기 (`sec:`)

`thymeleaf-extras-springsecurity`로 화면에서 인증/권한 분기:
```html
<div sec:authorize="isAuthenticated()"> 로그인 사용자만 </div>
<div sec:authorize="!isAuthenticated()"> 게스트만 </div>
<a th:if="${#authentication.principal.admin}" th:href="@{/admin/dashboard}">관리자</a>
```
로그인 사용자 정보: `${#authentication.principal.email}`, `.name`, `.profileImage`, `.admin` 등.

---

## 7. Tabler UI 스니펫 (1.4)

이 프로젝트는 [Tabler](https://tabler.io)(Bootstrap 5 기반)를 씁니다. 자주 쓰는 것:

```html
<!-- 카드 -->
<div class="card"><div class="card-header">제목</div><div class="card-body">...</div></div>

<!-- 버튼 -->
<button class="btn btn-primary">기본</button>
<button class="btn btn-danger">위험</button>
<button class="btn btn-ghost-secondary">보조</button>

<!-- 배지 -->
<span class="badge bg-blue-lt">정보</span>
<span class="badge bg-green-lt">성공</span>
<span class="badge bg-red-lt">위험</span>

<!-- 아바타 (프로필 이미지 없을 때 기본 이미지) -->
<span class="avatar" th:style="'background-image: url(' + (${u.profileImage != null}
     ? (${r2PublicUrl} + '/user/profile_image/100/' + ${u.profileImage})
     : '/image/no_profile_image.webp') + ')'"></span>
```

---

## 8. 주의 / 금지

- **인라인 JS 최소화** — 동작은 가능한 `hx-*` 속성으로. 꼭 필요한 스크립트는 `layout:fragment="js"`에.
- **CDN 의존** — Tabler/HTMX는 CDN 로드(`commons.html`). 폐쇄망 배포 시 정적 파일로 교체 필요.
- 리치 텍스트(약관/개인정보) 출력은 `th:utext`(이스케이프 안 함) 사용 — 단, **신뢰된 관리자 입력**에만. 사용자 입력은 `th:text`로 이스케이프.
- 콘텐츠(약관/개인정보) HTML 작성 규약은 **[03-content-format.md](03-content-format.md)** 참고.
