# BP 보일러플레이트

> 기존 **PHP/Laravel** 보일러플레이트(`bp_latest_ko_2026`)를 **Spring Boot + Java**로 1:1 포팅한 프로젝트입니다.
---

## 0. 한눈에 보기 (TL;DR)

| 항목 | 값 |
|---|---|
| 런타임 | **Java 21**, Spring Boot 3.2.5 |
| 화면 | 서버 렌더링(Thymeleaf) + **HTMX** (SPA 아님) |
| DB 접근 | **MyBatis** (XML 매퍼) + MySQL 8.0 |
| 스키마 관리 | **Flyway** 마이그레이션 |
| 인증/인가 | **Spring Security** (세션 기반) |
| 세션 저장 | **Spring Session JDBC** (DB에 세션 저장) |
| 동시성 | **Virtual Threads**(Java 21 Loom) 활성 |

핵심 철학: **"Laravel처럼 서버가 HTML을 그려서 내려주되, 부분 갱신은 HTMX로"**

---

## 1. Laravel ↔ Spring 개념 대응표

| 역할 | Laravel (원본) | 이 프로젝트 (Spring) |
|---|---|---|
| 언어/런타임 | PHP 8.x | **Java 21** |
| 프레임워크 | Laravel | **Spring Boot 3.2** |
| 의존성 관리 | Composer (`composer.json`) | **Maven** (`pom.xml`) |
| DB 접근 | Eloquent ORM | **MyBatis** (SQL을 XML에 직접 작성) |
| 모델 | `App\Models\User` (Eloquent) | `domain/User.java` (단순 POJO/record) + `mappers/*.xml` |
| 마이그레이션 | `database/migrations` (`php artisan migrate`) | **Flyway** `db/migration/V*.sql` (앱 기동 시 자동) |
| 시더(초기데이터) | `DatabaseSeeder` | **Flyway 시드 SQL** (`V4__seed_admin_and_settings.sql`) |
| 템플릿 | Blade (`*.blade.php`) | **Thymeleaf** (`*.html`) + Layout Dialect |
| 레이아웃 상속 | `@extends`/`@section` | `layout:decorate`/`layout:fragment` |
| 부분 컴포넌트 | `@include`, Livewire 컴포넌트 | Thymeleaf **fragment** + **HTMX** |
| 동적 UI | **Livewire** | **HTMX** (CDN 로드) |
| 인증 | `Auth` 파사드, `auth` 미들웨어 | **Spring Security** (필터 체인) |
| 인가 | 미들웨어/Gate/Policy | URL 규칙 + `hasRole` (`SecurityConfig`) |
| 세션 | `session()` (file/redis) | **Spring Session JDBC** (DB 테이블) |
| 검증 | `$request->validate()` | 컨트롤러 내 검증 + `PasswordPolicy` 등 |
| Rate Limit | `RateLimiter` 파사드 | **Bucket4j** (`RateLimiterService`) |
| 메일 | `Mail::send`, `MAIL_MAILER=ses` | **AWS SDK SES v2** (`MailService`) |
| 파일 저장 | `Storage` (S3 디스크) | **AWS SDK S3** → Cloudflare R2 (`R2StorageService`) |
| 이미지 처리 | Intervention Image | **Scrimage** (WebP 인코딩) |
| 비밀번호 해시 | `Hash::make` (bcrypt) | **BCryptPasswordEncoder(12)** |
| 캡차 | Turnstile | Turnstile (`TurnstileService`) |
| 설정 | `.env` + `config/*.php` | **`application.yml`** + `@ConfigurationProperties` |
| DI(의존성 주입) | 서비스 컨테이너 | Spring IoC 컨테이너 (생성자 주입) |
| 라우팅 | `routes/web.php` | 컨트롤러의 `@GetMapping`/`@PostMapping` 어노테이션 |
| 미들웨어 | HTTP 미들웨어 | 서블릿 필터 / Security 필터 체인 |
| 로그 뷰어 | `opcodes/log-viewer` 패키지 | 자체 최소 뷰어(`AdminLogsController`, 파일 tail) |

> **핵심 사고 전환 2가지**
> 1. **라우팅이 파일이 아니라 어노테이션**입니다. `routes/web.php` 대신 컨트롤러 메서드 위의 `@GetMapping("/...")`이 곧 라우트입니다.
> 2. **SQL을 직접 씁니다.** Eloquent의 "마법"이 없습니다. 매퍼 인터페이스(`UserMapper.java`)의 메서드가 XML(`UserMapper.xml`)의 SQL과 namespace로 연결됩니다.

---

## 2. 설정 방식 차이 (`.env` → `application.yml`)

Laravel의 `.env` + `config/*.php` 역할을 **`application.yml`** 하나가 합니다.

```yaml
# application.yml (모든 프로파일 공통)
spring:
  datasource:
    url: jdbc:mysql://${DB_HOST:localhost}:3306/${DB_DATABASE:bp}?...
    username: ${DB_USERNAME:root}
    password: ${DB_PASSWORD:...}
app:                       # @ConfigurationProperties("app")로 타입 안전하게 바인딩
  ses:
    access-key-id: ${SES_ACCESS_KEY_ID:}
  mail:
    from-address: ${MAIL_FROM_ADDRESS:crm@mig.kr}
```

- **`${ENV_VAR:기본값}`** 문법 = Laravel의 `env('ENV_VAR', '기본값')`과 동일. 환경변수 있으면 그 값, 없으면 기본값.
- **프로파일** = Laravel의 `APP_ENV`. `application-local.yml`(개발), `application-prod.yml`(운영)이 공통 `application.yml`을 **덮어씁니다**. 활성 프로파일은 `APP_ENV`(기본 `local`).
- **`@ConfigurationProperties`**: `app.*` 설정을 `AppProperties.java`(record)에 타입 안전하게 바인딩. Laravel의 `config('app.xxx')`보다 강타입.

| 환경 | 파일 | 특징 |
|---|---|---|
| 공통 | `application.yml` | DB, MyBatis, Flyway, 세션, SES, 가상스레드 등 |
| 로컬 | `application-local.yml` | 스키마 자동생성, Thymeleaf 캐시 끔, DEBUG 로그, 파일 로깅 |
| 운영 | `application-prod.yml` | Thymeleaf 캐시 켬, 시크릿은 환경변수 강제, 프록시 헤더 신뢰 |

---

## 3. 디렉터리 구조 (Laravel 대비)

```
src/main/java/com/example/bp/
├─ BpApplication.java          # public/index.php + bootstrap (진입점)
├─ config/                     # config/*.php + 미들웨어 등록 (SecurityConfig, WebConfig...)
├─ web/                        # app/Http/Controllers
│   ├─ home/   (공개)          # /, /contact, /signin, /privacy ...
│   ├─ client/ (로그인 사용자)  # /client/profile, /client/password
│   ├─ admin/  (관리자)        # /admin/**
│   └─ exception/              # 전역 예외 처리 (@ControllerAdvice)
├─ service/                    # app/Services (비즈니스 로직)
├─ security/                   # 인증 관련 (SecurityPrincipal, AuthSessionService...)
├─ domain/                     # app/Models (단, ORM 아님 — 순수 데이터)
├─ mapper/                     # MyBatis 인터페이스 (Repository 역할)
└─ support/                    # 헬퍼 (RateLimiter, PasswordPolicy...)

src/main/resources/
├─ templates/                  # resources/views (Blade → Thymeleaf)
│   ├─ layouts/                # 레이아웃 (app/admin/auth/error)
│   ├─ fragments/              # 재사용 조각 (navbar, footer, toasts, commons...)
│   ├─ home/ client/ admin/    # 페이지
│   └─ emails/                 # 메일 템플릿
├─ mappers/                    # MyBatis SQL XML
├─ db/migration/               # Flyway 마이그레이션 (database/migrations)
└─ application*.yml            # .env + config/*.php
```

---

## 4. 보안 설정 & 인증/인가 (소스코드 설명) ⭐

Laravel은 `auth` 미들웨어 + `Auth` 파사드가 자동으로 해주지만, Spring은 **필터 체인에서 인가**하고 **인증은 직접** 짭니다. 이 프로젝트의 핵심 설계입니다.

### 4.1 인가(Authorization) — `config/SecurityConfig.java`

URL 패턴별 접근 규칙을 한곳에서 정의합니다 (Laravel 라우트 미들웨어 그룹에 해당).

```java
http.authorizeHttpRequests(auth -> auth
    .requestMatchers("/", "/signin", "/signin/**", "/contact", "/privacy", "/terms", "/sitemap.xml").permitAll()
    .requestMatchers("/css/**", "/js/**", "/image/**", "/branding/**", "/theme/**", "/favicon.ico", "/error").permitAll()
    .requestMatchers("/admin/**").hasRole("ADMIN")     // 관리자만
    .requestMatchers("/client/**").authenticated()      // 로그인 필요
    .anyRequest().authenticated())                      // ★ 기본 차단 (fail-safe)
```

> **Laravel과 가장 큰 차이 — fail-safe(기본 차단).** `anyRequest().authenticated()`가 있어서 **명시적으로 permitAll 하지 않은 모든 경로는 자동으로 인증 필요**입니다.

- 이 검사는 **컨트롤러 도달 전, 필터 단계**에서 일어납니다 (요청 파이프라인 최전선).
- 미인증 + 보호 자원 → `/signin`으로 리다이렉트(`formLogin`의 진입점 역할만). 인증됐지만 admin 아님 → **403** (`templates/error/403.html`).
- 비밀번호 인코더: `BCryptPasswordEncoder(12)` (Laravel `BCRYPT_ROUNDS=12`와 동일).

### 4.2 인증(Authentication) — 수동 로그인

**중요**: 이 프로젝트는 Spring의 자동 로그인을 쓰지 않고 **직접** 인증합니다. 이유는 로그인에 **rate-limit + Turnstile 캡차 + HTMX 부분 응답**을 끼우기 위해(= Livewire 레퍼런스와 동일 동작).

로그인 흐름 (`web/home/SigninController.java`의 `/signin/login`):

```java
// 1) 횟수 제한 (Bucket4j) — Laravel RateLimiter::tooManyAttempts 대응
RateLimiterService.Result limit = rateLimiter.attempt(key, 5, Duration.ofSeconds(60));
if (!limit.allowed()) return blocked(...);          // 5회/60초 초과 시 차단

// 2) 캡차 검증
if (!turnstileService.verify(turnstileToken, ip)) return turnstileFailed(...);

// 3) 자격증명 검증 → 세션 수립
try {
    Authentication auth = authSession.authenticate(email, password);   // 비번 대조
    String target = authSession.establishAndResolveTarget(auth, req, resp);
    response.setHeader("HX-Redirect", target);      // HTMX에게 "이 주소로 이동해" 지시
    return REDIRECT;
} catch (AuthenticationException e) {
    // 실패 → 오류 토스트가 담긴 카드 fragment 재렌더
}
```

실제 인증/세션 처리는 `security/AuthSessionService.java`가 담당:

```java
// 이메일+비번 검증 (틀리면 예외)
public Authentication authenticate(String email, String password) {
    return authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(email, password));
}

// 로그인 상태를 세션에 저장 + 이동 대상 URL 결정
public String establishAndResolveTarget(Authentication auth, HttpServletRequest req, HttpServletResponse resp) {
    String target = resolveTarget(...);        // 원래 가려던 URL 있으면 거기, 없으면 역할별 기본
    SecurityContext ctx = SecurityContextHolder.createEmptyContext();
    ctx.setAuthentication(auth);
    securityContextRepository.saveContext(ctx, req, resp);  // 세션에 저장
    request.changeSessionId();                  // 세션 고정 공격 방지 (★ Laravel session()->regenerate())
    return target;
}
```

### 4.3 "이메일 → 사용자" 조회 — `security/UserDetailsServiceImpl.java`

`authenticationManager.authenticate()`가 내부적으로 호출. Laravel의 `User::where('email', ...)`에 해당.

```java
public UserDetails loadUserByUsername(String email) {
    User user = userMapper.findByEmail(email);       // MyBatis 조회
    if (user == null) throw new UsernameNotFoundException(...);
    return SecurityPrincipal.from(user);             // 권한(ROLE) 포함한 인증 객체로 변환
}
```

- `SecurityPrincipal` = Laravel의 `Auth::user()`에 해당. 화면에서 `#authentication.principal.email` 등으로 접근.
- 비번 대조는 **Spring이** `BCryptPasswordEncoder.matches()`로 자동 수행.

### 4.4 횡단 데이터 주입 — `web/GlobalModelAttributes.java` (`@ControllerAdvice`)

Laravel의 `View::share()` / View Composer에 해당. **모든 화면**에 공통 데이터를 주입:
`appName`, `setting`(사이트 설정 싱글톤), `_csrf`(토큰), `success`/`error`(플래시 메시지), 로그인 사용자 등.

### 4.5 CSRF / 세션 / Rate Limit 요약

| 항목 | 구현 | Laravel 대응 |
|---|---|---|
| CSRF | Spring Security CsrfFilter + meta 태그 + `fragments/htmx_csrf.html`가 모든 HTMX 요청에 토큰 자동 첨부 | `@csrf`, `VerifyCsrfToken` |
| 세션 저장 | DB 테이블(`SPRING_SESSION`), Flyway가 DDL 관리 | `SESSION_DRIVER=database` |
| Rate Limit | Bucket4j 인메모리(단일 노드). 확장 시 Redis로 교체 | `RateLimiter` |
| 캡차 | Turnstile, local에선 자동 우회 | 동일 |

---

## 5. HTMX 프래그먼트 구현 방식 ⭐

Laravel + Livewire의 "컴포넌트가 알아서 부분 갱신"을 **HTMX + Thymeleaf fragment**로 재현합니다. 핵심은 **"서버가 HTML 조각을 내려주고, HTMX가 화면 일부를 바꿔치기"**.

### 5.1 큰 그림 (Livewire ↔ HTMX)

| Livewire | HTMX |
|---|---|
| `wire:click`, `wire:submit` | `hx-get`, `hx-post` |
| 컴포넌트가 다시 렌더 | 컨트롤러가 **fragment** 반환 → DOM 일부 교체 |
| `$this->dispatch('event')` | 응답 헤더 `HX-Trigger`, `HX-Redirect` |
| 자동 CSRF | meta 토큰 + `htmx_csrf.html` 스크립트 |

> HTMX는 **자바 의존성이 아닙니다.** 레이아웃의 CDN `<script src=".../htmx.min.js">` 한 줄로 로드됩니다(`fragments/commons.html`).

### 5.2 두 가지 렌더링 경로

**① 최초 로딩 (전체 페이지)** — 서버에서 레이아웃 조립:
- 페이지(`home/contact.html`)가 `layout:decorate="~{layouts/app}"`로 공통 레이아웃에 끼워짐.
- 레이아웃은 `th:replace="~{fragments/app_navbar :: navbar}"`로 navbar/footer/toasts 등을 끌어옴.
- **이 모든 게 서버에서 한 번에 합쳐져** 완성 HTML로 전송됩니다 (브라우저는 `th:*`를 절대 못 봄).

**② 이후 동작 (부분 갱신)** — 컨트롤러가 **fragment 이름만** 반환:
```java
// AdminSettingController
@PostMapping("/information")
public String saveInformation(...) {
    settingService.updateInformation(footer, version);
    model.addAttribute("success", "정보가 업데이트되었습니다.");
    informationModel(model);
    return "admin/setting/information :: card";   // ★ 전체 페이지 X, "card" 조각만 렌더
}
```
→ Thymeleaf가 `card` 조각만 HTML로 만들어 응답 → **HTMX가 그 조각을 화면의 타깃에 swap**.

### 5.3 fragment 정의 & 사용 (실제 예시)

`templates/admin/setting/information.html`:
```html
<div id="info-card" th:fragment="card">           <!-- ← "card" 조각 (id와 fragment 이름 짝) -->
    <th:block th:replace="~{fragments/message :: toasts}"></th:block>  <!-- 토스트 영역 -->
    <form hx-post="/admin/setting/information"
          hx-target="#info-card"                    <!-- 응답을 이 요소에 -->
          hx-swap="outerHTML">                       <!-- 통째로 교체 -->
        <input name="footer" th:value="${footer}">  <!-- model의 값으로 채워짐 -->
        ...
    </form>
</div>
```

데이터 전달의 핵심: **반환값(String)은 "뷰 이름"일 뿐, 실제 데이터는 `Model`로 전달**됩니다. (Blade에서 `view('x', $data)`의 `$data`가 Spring에선 `model`)

### 5.4 리다이렉트 & 토스트

- **리다이렉트가 필요하면** 전체 페이지 대신 `HX-Redirect` 헤더 + 빈 fragment(`fragments/empty :: empty`) 반환 → HTMX가 클라이언트에서 이동.
- **플래시 메시지**: `model.addAttribute("success", ...)` → `fragments/message :: toasts`가 우하단 토스트로 렌더. 리다이렉트 시엔 세션 플래시(`FlashMessage`)로 다음 페이지에서 표시.

### 5.5 공통 자산 DRY — `fragments/commons.html`

Tabler CSS/JS, HTMX 버전이 한곳에만 있어, **버전 업그레이드 시 이 파일만** 고치면 됩니다. 각 레이아웃은 `th:replace="~{fragments/commons :: head}"` 등으로 필요한 조각만 가져옵니다.

---

## 6. DB 마이그레이션 & 시드 (Flyway)

Laravel `php artisan migrate` 대신 **Flyway**가 앱 기동 시 자동 실행.

- 파일: `db/migration/V1__create_users.sql`, `V2__...`, ... (이름 규칙 `V{버전}__{설명}.sql`)
- **순서대로, 한 번씩만** 실행. 적용 이력은 `flyway_schema_history` 테이블에 기록.
- 초기 데이터(관리자 계정 등)는 `V4__seed_admin_and_settings.sql`이 담당 (Laravel Seeder 역할). `NOT EXISTS`로 멱등 처리.

> ⚠️ **이미 적용된 마이그레이션 파일은 절대 수정 금지** (주석조차도). 내용이 곧 checksum이라, 바꾸면 기동 시 "checksum mismatch"로 실패합니다. 변경이 필요하면 새 `V5__...sql`을 추가하세요. (로컬은 `validate-on-migrate: false`로 완화해 둠.)

---

## 7. 이번 포팅/정리 작업에서 바뀐 것들 (Changelog)

- **시드 방식 변경**: Java `DataInitializer`(ApplicationRunner) → **Flyway 시드 마이그레이션**(`V4`)으로 일원화.
- **메일 발송**: SES **SMTP** → **SES API(SDK, sesv2)**로 전환. IAM 액세스 키를 그대로 사용(별도 SMTP 자격증명 불필요). `MailService`가 `app.ses` 키 유무로 SES API/SMTP/로그를 자동 선택.
- **가상 스레드 활성화**: `spring.threads.virtual.enabled: true` (Java 21 Loom) — 블로킹 I/O 동시 처리량 향상, 코드 변경 0.
- **라우팅 정리**: 컨트롤러에 클래스 레벨 `@RequestMapping("/admin/user")` 등 공통 경로 도입 → 메서드는 상대 경로.
- **공통 레이아웃 조각화**: `fragments/commons.html` 신설로 Tabler/HTMX 버전 중복 제거.
- **전역 예외 처리**: `@ControllerAdvice`(`GlobalExceptionHandler`) + `CardException`/`BusinessException`으로 HTMX/전체페이지 분기. (단, 폼 상태가 복잡한 signin/contact는 인라인 유지가 더 명확하여 그대로 둠.)
- **파일 로깅 + 로그 뷰어**: 로컬에서도 `logs/bp.log` 기록 → 관리자 로그 화면에서 tail 확인.
- **기타**: 파비콘, 기본 프로필 이미지(`/image/no_profile_image.webp`), 시스템 메뉴명(SystemInfo), 주석 한국어화, MyBatisConfig/AsyncConfig 정리.

---

## 8. 로컬 실행 방법

**사전 요구사항**: JDK 21, MySQL 8.0(localhost:3306, DB명 `bp`).

```bash
# 1) MySQL 실행 (스키마는 local 프로파일이 자동 생성)
# 2) 앱 실행
./mvnw spring-boot:run          # 또는 IDE에서 BpApplication 실행

# 접속: http://localhost:8080
# 초기 관리자: admin@example.com / password1!  (운영 배포 전 반드시 변경)
```

테스트:
```bash
./mvnw test
```

---

## 9. 운영 전 체크리스트 (보안/주의)

- [ ] **시크릿을 환경변수로** — DB 비번, `SES_ACCESS_KEY_ID/SECRET`, Turnstile 키 등은 `application.yml` 기본값에 박지 말고 환경변수로 주입. (yml에 들어간 실제 키는 **커밋 금지**, 노출됐으면 **즉시 회전**.)
- [ ] **시드 관리자 비밀번호 변경** (`admin@example.com` / `password1!`).
- [ ] **Flyway 마이그레이션 파일 수정 금지** (새 버전 추가로만 변경).
- [ ] **SES**: 발신 도메인/주소 검증 + 샌드박스 해제 여부 확인. 문의 수신함은 `CONTACT_TO_ADDRESS`로 지정.
- [ ] **Turnstile**: 운영은 site/secret 키 필수 (local은 자동 우회, prod에서 secret 비면 fail-open).
- [ ] **Rate Limit**: 다중 노드로 확장 시 Bucket4j 인메모리 → Redis 등 분산 저장소로 교체. 프록시 뒤면 실제 클라이언트 IP 전달(`forward-headers-strategy`) 확인.

---

## 부록: 더 자세한 요청 흐름

라우트별 컨트롤러→서비스→매퍼(XML)→템플릿 전체 매핑은 **[docs/REQUEST_FLOW.md](docs/REQUEST_FLOW.md)** 참고.
