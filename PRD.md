# PRD — Spring Boot 보일러플레이트 업그레이드

> **목적**: 기존 *Spring Boot + Maven + MyBatis + Thymeleaf* 보일러플레이트를, 레퍼런스인 **BP LATEST KO 2026 (Laravel 13 + Livewire 4)** 보일러플레이트와 **기능 동등(feature parity)** 수준으로 업그레이드한다.
>
> **대상 독자**: 본 PRD를 구현하는 개발자 / Claude Code.
> **레퍼런스 원본**: 이 저장소(`app/Livewire/*`, `routes/web.php`, `docs/*`).

---

## 1. 개요 & 결정사항

### 1.1 배경
한국어 웹앱을 빠르게 시작하기 위한 보일러플레이트를 Laravel에서 Spring 스택으로 이식한다. 페이지 단위 단순 구조, 빌드리스 프론트엔드, 서버 부분 반응성, 운영 도구(백업/로그/사용자관리)를 그대로 가져온다.

### 1.2 확정된 기술 결정
| 항목 | 결정 |
|------|------|
| **인터랙션 모델** | **HTMX + Thymeleaf 프래그먼트** — Livewire의 부분 반응성(페이지 리로드 없는 폼 검증/탭 전환/인라인 CRUD)을 서버 HTML 조각 반환으로 재현 |
| **외부 서비스** | **그대로 유지** — Cloudflare R2(S3 호환), AWS SES, Cloudflare Turnstile |
| **DB** | **MySQL / MariaDB** |
| **인증/인가** | **Spring Security (세션 기반 폼 로그인)** + role 기반 인가, BCrypt |
| **빌드 철학** | **빌드리스 프론트엔드 유지** — CSS/JS는 CDN(Tabler 1.4 = Bootstrap 5.3.7) + HTMX CDN. npm/webpack 없음 |

### 1.3 목표 (Goals)
- 레퍼런스의 **모든 페이지/기능**을 Spring 스택으로 1:1 재현.
- Livewire 컴포넌트 단위 = **Controller + Thymeleaf 템플릿 + HTMX 프래그먼트** 단위로 매핑.
- 보안 메커니즘(레이트리밋, OTP, Turnstile, role 가드)을 **동작 동등**하게 구현.
- 한국어 우선 / `Asia/Seoul` 타임존 / SEO·사이트맵 기본 제공.

### 1.4 비목표 (Non-Goals)
- OAuth 소셜 로그인 (레퍼런스도 미사용).
- SPA / 클라이언트 사이드 라우팅.
- npm 빌드 파이프라인 도입.
- Livewire의 자동 양방향 바인딩을 100% 모사 (HTMX 명시적 트리거로 대체).

---

## 2. 기술 스택 (Target)

| 레이어 | 기술 |
|--------|------|
| 런타임 | Java 21+ (LTS), Spring Boot 3.x |
| 빌드 | Maven |
| 웹/MVC | Spring MVC `@Controller` (HTML 반환) |
| 뷰 | Thymeleaf + `thymeleaf-layout-dialect` (레이아웃) |
| 부분 반응성 | **HTMX**(CDN), 필요한 곳에만 |
| 데이터 접근 | **MyBatis** (`mybatis-spring-boot-starter`) |
| DB | MySQL 8 / MariaDB |
| 스키마 버전관리 | **Flyway** (Laravel migration 대체) |
| 인증/인가 | Spring Security 6 (form login, session) |
| 세션 저장소 | **Spring Session JDBC** (레퍼런스의 DB 세션 드라이버 대응) |
| 객체 스토리지 | AWS SDK for Java v2 — S3 클라이언트(R2 엔드포인트) |
| 메일 | AWS SDK SES **또는** Spring `JavaMailSender`(SES SMTP) |
| 이미지 처리 | **Scrimage** 또는 `TwelveMonkeys ImageIO` + `webp-imageio` (WebP 인코딩) |
| 레이트리밋 | **Bucket4j** (IP/액션별) |
| HTTP 클라이언트 | `RestClient`/`WebClient` (Turnstile siteverify) |
| 스케줄러 | Spring `@Scheduled` |
| 비동기 | Spring `@Async` (또는 경량 작업 테이블) |
| 테스트 | JUnit 5 + Spring Boot Test + Testcontainers(MySQL) |
| UI 프레임워크 | Tabler 1.4 (CDN), 인라인 SVG 아이콘만 |

> **프론트엔드 원칙(레퍼런스 계승)**: 아이콘은 **인라인 SVG만**(웹폰트 금지), CSS/JS는 CDN, 페이지마다 `seo`/`css`/`js` 슬롯 + 단일 루트.

---

## 3. 아키텍처 원칙

### 3.1 페이지 단위 매핑 (1 page = 1 controller + 1 view)
레퍼런스의 "Route → Livewire → View"를 다음으로 치환한다:

```
Laravel                          Spring
─────────────────────────────────────────────────────────
Route::get('/x', Foo::class)  →  @GetMapping("/x") in FooController
Livewire public method        →  @PostMapping("/x/action") → 프래그먼트 반환
public $property (상태)        →  요청 파라미터 / 세션 / 모델 어트리뷰트
view('livewire.foo')          →  templates/foo.html
dispatch/On (이벤트)           →  HTMX 이벤트(hx-trigger) 또는 직접 호출
```

불필요한 Service/Mapper 추상화를 미리 만들지 않는다(실제 중복 발생 후 추출). 단, 외부 통합(R2/SES/Turnstile/Image)은 처음부터 `@Service`/`@Component`로 캡슐화한다.

### 3.2 HTMX 상호작용 표준 패턴
Livewire 액션 → HTMX 요청으로 변환하는 규칙:

| Livewire | HTMX 구현 |
|----------|-----------|
| `wire:submit="send"` | `<form hx-post="/contact" hx-target="#result" hx-swap="outerHTML">` → 서버가 결과 프래그먼트 반환 |
| `wire:click="setPage('Signup')"` | `hx-get="/signin?tab=signup" hx-target="#auth-card"` → 탭 프래그먼트 교체 |
| `wire:model.live`(실시간 검증) | `hx-post="/admin/user/check-email" hx-trigger="change" hx-target="#email-feedback"` |
| `wire:loading` | HTMX `hx-indicator` + Bootstrap 스피너 |
| `wire:confirm="..."` | `hx-confirm="정말 삭제하시겠습니까?"` |
| `dispatch('turnstile-reset')` | 응답 헤더 `HX-Trigger: turnstileReset` → JS 리스너가 위젯 리셋 |
| 페이지네이션 | `hx-get="/admin/user?page=2" hx-target="#user-table"` (프래그먼트) |
| RTE 저장 | 폼 POST(HugeRTE hidden input) |

**프래그먼트 컨벤션**: 각 템플릿은 `th:fragment`로 갱신 단위(폼 카드, 테이블, 메시지 영역)를 정의. 컨트롤러는 전체 페이지 또는 프래그먼트만 선택 반환(`return "user :: table"`).

### 3.3 레이아웃 (Thymeleaf Layout Dialect)
레퍼런스 4종 레이아웃을 동일하게:
- `layouts/app` — 공개(navbar + footer)
- `layouts/auth` — 컴팩트(로그인/약관)
- `layouts/admin` — 관리자(sidebar + topbar + footer)
- `layouts/error` — 오류 페이지

각 레이아웃은 `<head>`에 `seo`/`css` 슬롯, `</body>` 직전에 `js` 슬롯을 둔다. CDN 자산(Tabler, HTMX)은 레이아웃에서 로드.

### 3.4 패키지 구조(권장)
```
com.example.bp
 ├─ config/         # SecurityConfig, WebConfig, MyBatisConfig, AwsConfig, AsyncConfig
 ├─ web/            # @Controller (페이지 단위)
 │   ├─ home/       # HomeController, SigninController, ContactController, PrivacyController, TermsController
 │   ├─ client/     # ClientProfileController, ClientPasswordController
 │   └─ admin/      # AdminDashboard, AdminUser, AdminSetting*, AdminDevelopment*
 ├─ domain/         # User, Setting (도메인 객체 / record)
 ├─ mapper/         # MyBatis 인터페이스 (UserMapper, SettingMapper)
 ├─ security/       # UserDetailsServiceImpl, CustomAuthHandlers, RoleGuard
 ├─ service/        # R2StorageService, MailService, TurnstileService, ImageService, OtpService, BackupService
 ├─ support/        # RateLimiter, FlashMessage, SeoMeta
 └─ BpApplication.java
resources/
 ├─ mappers/        # *.xml (MyBatis SQL)
 ├─ db/migration/   # Flyway V*.sql
 ├─ templates/      # Thymeleaf (layouts/, fragments/, home/, client/, admin/, emails/, errors/)
 └─ application.yml
```

---

## 4. 외부 서비스 통합 요구사항

### 4.1 객체 스토리지 — Cloudflare R2 (2-버킷)
레퍼런스의 공개/비공개 분리를 그대로 유지.

| 디스크 | 버킷(env) | 접근 | 용도 |
|--------|-----------|------|------|
| `r2_public` | `R2_PUBLIC_BUCKET` | Custom Domain(`R2_PUBLIC_URL`) | 프로필 이미지 등 브라우저 직접 로드 |
| `r2_private` | `R2_PRIVATE_BUCKET` | presigned URL(`temporaryUrl`) | 백업 등 비공개 |

**`R2StorageService` 요구 메서드**:
- `putPublic(key, bytes, contentType)` → 공개 버킷 저장
- `publicUrl(key)` → `R2_PUBLIC_URL` + key
- `putPrivate(key, bytes)` / `presignedUrl(key, Duration)` (기본 15분)
- `delete(disk, key)`

AWS SDK v2 S3Client를 R2 엔드포인트(`R2_ENDPOINT`)·자격증명(`R2_ACCESS_KEY_ID/SECRET`)으로 구성, path-style 활성화.

> **판단 기준**: "브라우저가 `<img>/<a>`로 직접 로드하나?" → 예: public / 아니오: private.

### 4.2 메일 — AWS SES
- `MailService.sendHtml(to, toName, subject, html, replyTo?, attachment?)`.
- 사용처: ① OTP 비밀번호 재설정 메일, ② 문의 접수 메일(첨부 포함, replyTo=문의자).
- 발신 주소/이름은 설정(`MAIL_FROM_*`), 문의 수신자는 `CONTACT_TO_ADDRESS/NAME`.

### 4.3 봇 방지 — Cloudflare Turnstile
`TurnstileService`:
- `enabled()` — 로컬 프로파일이면 항상 `false`; 운영에서 site/secret 모두 설정 시 `true`.
- `verify(token, remoteIp)` — 로컬이면 `true`; secret 미설정이면 `true`(fail-open); 그 외 siteverify 호출.
- 적용 지점: 로그인, 회원가입, 문의.
- 뷰에서 `enabled()` 체크 후 위젯 렌더링, 실패 시 `HX-Trigger: turnstileReset`.

### 4.4 프록시 신뢰
Cloudflare 뒤 배포 → `server.forward-headers-strategy=NATIVE`(또는 framework) 설정으로 실제 client IP/scheme/host 복원. 레이트리밋·Turnstile이 실 IP를 쓰도록 보장.

---

## 5. 데이터 모델 (MySQL + MyBatis)

### 5.1 `users`
| 컬럼 | 타입 | 비고 |
|------|------|------|
| id | BIGINT PK AUTO_INCREMENT | |
| role | VARCHAR | `client` \| `admin` (기본 `client`) |
| email | VARCHAR UNIQUE | |
| email_verified_at | DATETIME NULL | |
| name | VARCHAR NULL | |
| password | VARCHAR | BCrypt 해시 |
| otp | VARCHAR NULL | 재설정 OTP |
| otp_expires_at | DATETIME NULL | |
| otp_attempts | INT DEFAULT 0 | 시도 횟수 |
| profile_image | VARCHAR NULL | R2 상 파일명 |
| remember_token | VARCHAR NULL | (remember-me 사용 시) |
| created_at / updated_at | DATETIME | |

### 5.2 `settings` (**단일 행 싱글톤**)
| 컬럼 | 타입 | 비고 |
|------|------|------|
| id | BIGINT PK | |
| footer | VARCHAR | 푸터 문구 |
| version | VARCHAR | 앱 버전 |
| terms | LONGTEXT | 이용약관(HTML) |
| privacy | LONGTEXT | 개인정보처리방침(HTML) |
| created_at / updated_at | DATETIME | |

> 접근 시 `firstOrNew` 시맨틱: 행 없으면 기본 1행 생성. `SettingService.get()`/`save()`로 캡슐화.

### 5.3 부가 테이블
- `SPRING_SESSION`, `SPRING_SESSION_ATTRIBUTES` — Spring Session JDBC 스키마.
- (선택) `jobs`/작업 테이블 — 비동기 큐 사용 시.
- Flyway `flyway_schema_history`.

### 5.4 시드 데이터
- 초기 관리자: `admin@example.com` / `password1!` (BCrypt).
- `settings` 1행(footer/version 기본값).
- Flyway `R__seed` 또는 `ApplicationRunner`로 멱등 주입. **운영 배포 전 비밀번호 변경 경고** 명시.

---

## 6. 인증 & 인가 (Spring Security)

### 6.1 구성
- `SecurityFilterChain`: form login(`/signin`), 세션 기반, CSRF 활성(HTMX 헤더 포함).
- `UserDetailsServiceImpl`: email → `users` 조회, role을 권한(`ROLE_ADMIN`/`ROLE_CLIENT`)으로 매핑.
- `PasswordEncoder`: BCrypt (cost 12, 레퍼런스 `BCRYPT_ROUNDS=12`).
- 인가 규칙:
  - `/`, `/signin`, `/contact`, `/privacy`, `/terms`, `/sitemap.xml`, 정적자원 → permitAll
  - `/client/**` → authenticated
  - `/admin/**` → `hasRole('ADMIN')` (레퍼런스 `check_admin`)
  - 미인증 admin 접근 → `/signin` 리다이렉트(원래 URL 보존 = `intended`)
  - 인증됐으나 비관리자 → **403**

### 6.2 로그인 성공 후 리다이렉트
`AuthenticationSuccessHandler`: 보존된 원래 URL이 있으면 그곳으로, 없으면 role별 기본(admin→`/admin/dashboard`, client→`/`).

### 6.3 비밀번호 정책 (공통, 레퍼런스 동일)
- 8~15자, **소문자·대문자·숫자·특수문자(`!@#$%^&*`) 각 1개 이상**.
- 정규식: `^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[!@#$%^&*])[A-Za-z\d!@#$%^&*]{8,15}$`
- 공통 검증 유틸 + 동일 한국어 에러 메시지.

### 6.4 로컬 전용 퀵 로그인
`local` 프로파일에서만 노출되는 admin/client 원클릭 로그인 엔드포인트(운영 비활성).

---

## 7. 기능 요구사항 (Functional Requirements)

> 각 항목은 **레퍼런스 컴포넌트 → Spring 구현 → 수용 기준** 순. HTMX 동작을 명시.

### FR-1. 공개: 랜딩 `/` (`Home`)
- `GET /` → `home/index.html`(layout `app`). 정적 랜딩.

### FR-2. 공개: 통합 인증 `/signin` (`HomeSignin`) — 핵심
하나의 페이지에서 **로그인 / 회원가입 / 비밀번호 재설정** 3-in-1. 탭 전환은 HTMX 프래그먼트 교체(모달 아님).

**FR-2.1 로그인**
- `POST /signin/login` (email, password, turnstileToken).
- 레이트리밋: **IP+`login` 60초당 5회**. 초과 시 한국어 안내 + `turnstileReset`.
- Turnstile 검증 → 실패 시 에러 프래그먼트.
- 인증 성공 시 role별 리다이렉트(§6.2). HTMX는 `HX-Redirect` 헤더로 처리.

**FR-2.2 회원가입**
- `POST /signin/signup` (email[unique], name, password).
- 레이트리밋: **5분당 3회**. 비밀번호 정책 검증. Turnstile.
- 성공 시 즉시 로그인 + 리다이렉트.

**FR-2.3 비밀번호 재설정 (이메일 OTP)** — `OtpService`로 캡슐화
1. `POST /signin/reset/send` (email): 레이트리밋 **5분당 3회**. 사용자 존재 시 **6자리 OTP** 생성, **만료 10분**, `otp_attempts=0`, SES 발송. 메일 템플릿 `emails/password_reset.html`.
2. `POST /signin/reset/verify` (email, code): 레이트리밋 **10분당 10회**. 검증 규칙:
   - 만료 시 OTP 무효화 + 에러.
   - **시도 5회 초과** 시 OTP 무효화 + 에러.
   - **상수 시간 비교**(`MessageDigest.isEqual`, 타이밍 공격 방지). 불일치 시 `otp_attempts++` + 남은 횟수 안내.
   - 일치 시 검증 상태 통과(세션 플래그).
3. `POST /signin/reset/password` (email, password): 검증 통과 상태에서만 새 비밀번호 저장, `email_verified_at=now`, OTP 무효화, 자동 로그인.

**FR-2.4 로컬 퀵 로그인 / 비밀번호 표시 토글** (§6.4).

- **수용 기준**: 5개 액션 각각 레이트리밋·Turnstile·검증이 레퍼런스 임계값과 동일하게 동작. OTP 만료·시도제한·상수시간 비교 단위 테스트 통과.

### FR-3. 공개: 문의 `/contact` (`HomeContact`)
- `GET /contact` → 폼(name, email, subject, message, file).
- `POST /contact`: 검증(message ≤5000자, file ≤10MB nullable). 레이트리밋 **IP당 10분에 3건**. Turnstile.
- SES 발송: 수신자 `CONTACT_TO_ADDRESS`(없으면 from), **replyTo=문의자**, 제목 `[문의] {subject} — {name}`, 본문 HTML(XSS 이스케이프). **첨부는 저장하지 않고 메일에만 첨부**.
- 성공 시 폼 리셋 + 성공 메시지 프래그먼트.

### FR-4. 공개: 약관/개인정보 `/terms` `/privacy`
- `settings.terms` / `settings.privacy` HTML을 그대로 렌더(레이아웃 `auth` 또는 `app`).

### FR-5. 공개: 사이트맵 `/sitemap.xml`
- `GET /sitemap.xml` → `application/xml`, 공개 라우트 목록 렌더(`templates/sitemap/index.xml`).

### FR-6. 클라이언트 영역 (`/client/**`, authenticated)
- **FR-6.1 `/client/profile`** — 프로필 이미지 업로드/삭제(§FR-8.2와 동일 파이프라인).
- **FR-6.2 `/client/password`** — 비밀번호 변경(현재 비번 확인 + 정책 검증).

### FR-7. 관리자: 대시보드 `/admin/dashboard`
- 관리자 진입점(요약 카드). 최소 구현 OK.

### FR-8. 관리자: 계정
- **FR-8.1 `/admin/account/password`** — 비밀번호 변경.
- **FR-8.2 `/admin/account/profile`** — 프로필 이미지:
  - 업로드 검증: image, jpg/jpeg/png, ≤2MB.
  - **이미지 파이프라인**: 디코드 → `cover` 정사각 크롭 → **WebP(quality 85)로 100px·400px 2종** 인코딩 → R2 public 저장(`user/profile_image/{100,400}/{file}.webp`).
  - 기존 이미지 교체 시 이전 파일 삭제. 삭제 액션 별도.

### FR-9. 관리자: 사용자 관리 `/admin/user` (`AdminUser`)
- 목록: **검색**(숫자면 id 정확매칭, 아니면 email/name LIKE), **role 필터**(all/client/admin), **페이지네이션 10개**(HTMX로 테이블 프래그먼트 교체).
- **생성**: 이메일 중복확인(HTMX live, `POST /admin/user/check-email`) → 이름·비밀번호 정책 검증 → 저장(`email_verified_at=now`).
- **수정**: 이름/role 수정, 비밀번호는 입력 시에만 변경.
- **삭제**: `hx-confirm`.
- **impersonate**: 해당 사용자로 즉시 로그인 후 홈 이동.
- MyBatis: 동적 `<where>`로 검색/필터, `LIMIT/OFFSET` 페이지네이션.

### FR-10. 관리자: 설정
- **FR-10.1 `/admin/setting/information`** — footer/version 편집(싱글톤 저장).
- **FR-10.2 `/admin/setting/privacy`**, **FR-10.3 `/admin/setting/terms`** — **HugeRTE**(CDN) 리치텍스트 에디터로 HTML 편집, settings 싱글톤 저장.
- **FR-10.4 `/admin/setting/branding`** — 로고 8종(SVG/PNG × color/white × square/wide) 업로드.
  - SVG: 그대로 `public/branding/`(또는 정적 경로)에 저장.
  - **정사각 컬러 PNG → 다중 사이즈 PNG(512·192·180·150·32·16) 자동 생성 + `favicon.ico` 자동 갱신**.
  - 정사각 화이트 PNG → 512px. wide PNG는 원본 저장.

### FR-11. 관리자: 개발 도구 (`/admin/development/**`)
- **FR-11.1 `/admin/development/backup`** — 백업 생성/목록/다운로드/삭제. `BackupService`(예: `mysqldump` + zip → r2_private 또는 로컬). 다운로드는 인증된 관리자만.
- **FR-11.2 `/admin/development/logs`** — 로그 뷰어. (opcodesio/log-viewer 대체: 간단한 로그 파일 뷰어 또는 Spring Boot Admin/외부 도구 안내. **최소**: 최근 로그 파일 tail 뷰.)
- **FR-11.3 `/admin/development/php` 대체** — 런타임/환경 정보 페이지(`/admin/development/system`): JVM/Spring 환경, 메모리, 활성 프로파일 표시.
- **FR-11.4 DB 콘솔(`AdminDevelopmentDatabase`)** — ⚠️ **선택/주의**: 레퍼런스는 테이블 생성·컬럼 추가/삭제(마이그레이션 파일 직접 수정)·행 CRUD가 가능한 강력한 도구. Spring에선 Flyway 기반 스키마와 충돌하므로 **기본은 읽기 전용 테이블 브라우저 + 행 조회**로 축소 구현하고, 쓰기 기능은 보안 검토 후 별도 결정.

---

## 8. 횡단 관심사 (Cross-Cutting)

### 8.1 SEO
- 페이지별 `seo` 슬롯: `<title>`, description, keywords, OG, twitter card.
- `SeoMeta` 헬퍼/모델 어트리뷰트로 표준화.

### 8.2 플래시 메시지
- success/error 플래시 → Spring `RedirectAttributes` 또는 세션. 공통 `fragments/message.html`로 렌더. HTMX 응답은 메시지 영역을 OOB swap(`hx-swap-oob`)으로 갱신 가능.

### 8.3 레이트리밋 (Bucket4j)
액션별 키 = `IP + action`. 레퍼런스 임계값을 그대로:

| 액션 | 제한 |
|------|------|
| login | 60초 5회 |
| signup | 5분 3회 |
| reset-send | 5분 3회 |
| reset-verify | 10분 10회 |
| contact | 10분 3건 |

초과 시 한국어 안내 + `turnstileReset`.

### 8.4 이미지 파이프라인
공통 `ImageService`: 디코드 → `cover(w,h)` 크롭 → WebP/PNG 인코딩 → bytes. 프로필(WebP), 브랜딩(PNG/favicon)에서 재사용.

### 8.5 파일 업로드 임시 저장
멀티파트 임시 저장은 로컬 디스크(`storage/tmp` 상당)로. (레퍼런스가 Cloudflare 프록시 이슈로 R2 임시저장을 우회한 것과 동일 취지 — Spring 멀티파트는 기본 로컬 temp이므로 자연스럽게 충족.)

### 8.6 i18n / 타임존
- 기본 로케일 `ko`, 메시지 번들 `messages_ko.properties`.
- JVM/앱 타임존 `Asia/Seoul`(`spring.jackson.time-zone`, DB 연결 `serverTimezone`).

### 8.7 오류 페이지
404 / 500 / 503 커스텀 템플릿(layout `error`).

---

## 9. 보안 요구사항
- 모든 폼에 **CSRF** 토큰(Thymeleaf 자동 + HTMX `X-CSRF-TOKEN` 헤더 설정).
- 사용자 입력 출력 시 **XSS 이스케이프**(Thymeleaf `th:text` 기본 이스케이프). 단, 약관/개인정보 HTML은 신뢰된 관리자 입력 → `th:utext` 렌더(관리자 전용 입력임을 문서화).
- 비밀번호 BCrypt, OTP 상수시간 비교, 레이트리밋(§8.3).
- presigned URL 만료(기본 15분).
- 관리자 도구(백업 다운로드/DB 콘솔) 접근통제 엄격.
- 운영 프로파일에서 퀵 로그인·개발도구 노출 차단.

---

## 10. 운영 (Queue / Schedule / Backup / Log)
- **스케줄러**: Spring `@Scheduled`(cron). 예: 백업 정리(daily), 백업 실행(daily 02:00). 레퍼런스의 `Schedule::command(...)` 대체.
- **비동기/큐**: 우선 `@Async`로 메일 발송 등 처리. 대량/재시도 필요 시 작업 테이블 또는 메시지 브로커로 확장(문서화만, 초기엔 @Async).
- **백업**: `mysqldump` 기반 zip → r2_private(또는 로컬). 보존 정책(오래된 백업 정리).
- **로그**: 파일 로깅(logback) + 관리자 뷰어(FR-11.2).

---

## 11. 환경 변수 (application.yml / .env)
레퍼런스 `.env`와 동등 키:
```
APP_NAME, APP_ENV(profile), APP_URL, APP_LOCALE=ko, TZ=Asia/Seoul
DB_HOST/PORT/DATABASE/USERNAME/PASSWORD
R2_ACCESS_KEY_ID, R2_SECRET_ACCESS_KEY, R2_ENDPOINT
R2_PUBLIC_BUCKET, R2_PUBLIC_URL, R2_PRIVATE_BUCKET
TURNSTILE_SITE_KEY, TURNSTILE_SECRET_KEY
SES_ACCESS_KEY_ID, SES_SECRET_ACCESS_KEY, SES_DEFAULT_REGION
MAIL_FROM_ADDRESS, MAIL_FROM_NAME
CONTACT_TO_ADDRESS, CONTACT_TO_NAME
```
Spring 프로파일: `local`(Turnstile/개발도구/퀵로그인), `prod`.

---

## 12. Laravel → Spring 매핑 표 (요약)

| 레퍼런스(Laravel/Livewire) | Spring 대응 |
|----------------------------|-------------|
| Livewire Component | `@Controller` + Thymeleaf + HTMX 프래그먼트 |
| Livewire public method | `@PostMapping`/`@GetMapping` 핸들러 |
| `wire:model`/`wire:click`/`wire:submit` | HTMX `hx-*` 속성 |
| Eloquent Model | 도메인 record + MyBatis Mapper |
| Migration | Flyway `V*.sql` |
| Middleware(auth/check_admin) | Spring Security 인가 규칙 |
| `RateLimiter` | Bucket4j |
| Intervention Image | Scrimage / ImageIO + webp-imageio |
| `Storage`(R2) | AWS SDK S3Client(`R2StorageService`) |
| `Mail`(SES) | SES SDK / JavaMailSender(`MailService`) |
| `Turnstile` 헬퍼 | `TurnstileService` |
| `Auth::attempt/login` | Spring Security AuthenticationManager / 세션 |
| `WithPagination` | MyBatis LIMIT/OFFSET + HTMX 프래그먼트 |
| `session()->flash` | RedirectAttributes / OOB swap |
| `dispatch/On` 이벤트 | HTMX `hx-trigger` / `HX-Trigger` 헤더 |
| spatie/laravel-backup | `BackupService`(mysqldump) |
| opcodesio/log-viewer | 로그 파일 뷰어(FR-11.2) |
| HugeRTE | HugeRTE(CDN, 동일) |
| Tabler 1.4 CDN | 동일 |

---

## 13. 구현 로드맵 (단계별)

> 각 단계는 독립 PR 가능. 위에서 아래로 의존.

1. **기반**: Maven 의존성, 프로파일/`application.yml`, Flyway 스키마(users/settings/session), MyBatis 설정, 시드.
2. **보안/인증**: Spring Security 세션, UserDetailsService, role 인가, 레이아웃 4종, 공통 프래그먼트(message/seo).
3. **인증 페이지 FR-2**: 통합 signin(로그인/가입/OTP 재설정) + 레이트리밋 + Turnstile + 퀵로그인.
4. **공개 페이지**: 랜딩, 문의(FR-3, SES), 약관/개인정보(FR-4), 사이트맵(FR-5).
5. **외부 통합 서비스**: R2StorageService, MailService, TurnstileService, ImageService(공통화).
6. **클라이언트 영역 FR-6**: 프로필 이미지(WebP 파이프라인), 비밀번호 변경.
7. **관리자 핵심**: 대시보드, 계정(FR-8), **사용자 관리 FR-9**(검색/필터/페이지네이션/CRUD/impersonate, HTMX).
8. **관리자 설정 FR-10**: information, privacy/terms(HugeRTE), branding(favicon 자동생성).
9. **개발 도구 FR-11**: backup, logs, system info, (선택) DB 브라우저.
10. **운영/마무리**: 스케줄러, 백업 정리, 오류 페이지, i18n/타임존, SEO 점검, 테스트(인증/OTP/레이트리밋/이미지).

---

## 14. 수용 기준 (Acceptance Criteria) — 핵심
- [ ] 비관리자가 `/admin/**` 접근 시 403, 미인증 시 `/signin`으로 리다이렉트 후 로그인하면 원래 URL 복원.
- [ ] 로그인/가입/OTP/문의의 레이트리밋이 §8.3 임계값과 동일하게 동작.
- [ ] OTP: 10분 만료, 6자리, 5회 시도제한, 상수시간 비교 — 단위 테스트 통과.
- [ ] 비밀번호 정책 정규식이 레퍼런스와 동일하게 강제.
- [ ] 프로필 업로드가 WebP 100/400px 2종 생성 + 기존 파일 삭제.
- [ ] 브랜딩 정사각 PNG 업로드 시 다중 PNG + favicon 자동 생성.
- [ ] 사용자 관리: 검색(id 정확/텍스트 LIKE)·role 필터·10개 페이지네이션·CRUD·impersonate가 페이지 리로드 없이(HTMX) 동작.
- [ ] 약관/개인정보가 HugeRTE로 편집·저장되고 공개 페이지에 렌더.
- [ ] 로컬 프로파일에서만 퀵로그인·개발도구 노출.
- [ ] R2 공개/비공개 버킷 분리 동작(공개=CDN URL, 비공개=presigned).
- [ ] 한국어 UI / Asia/Seoul / SEO 메타 / sitemap.xml 제공.

---

---

## 15. 화면 / UI 명세 (Screen Specifications)

> **§7(기능)이 "무엇을 하는가"라면, 본 섹션은 "어떻게 보이는가"다.** 레퍼런스의 모든 화면·UI 컴포넌트·토스트·문구를 1:1로 재현한다. (출처: `resources/views/**`)

### 15.1 공통 UI 시스템
- **UI 프레임워크**: Tabler 1.4 (Bootstrap 5.3.7) — CDN(`@tabler/core@1.4.0`). 빌드 없음.
- **아이콘**: **인라인 SVG만**(Tabler Icons SVG). 웹폰트 아이콘 전면 금지.
- **전역 CSS 규칙**(레이아웃 `<style>`에 포함):
  - 세로 스크롤바 발생 시 좌측 마진 제거: `html { margin-left: 0 !important; }`
  - 모든 `<a>` 밑줄 제거.
  - 일반 텍스트 링크는 부모 색상 상속(`color: inherit`), 단 `.btn` 버튼 링크는 Tabler 색상 유지(`a:not(.btn)`).
- **언어/메타**: `<html lang="ko">`, viewport `viewport-fit=cover`, canonical 링크, 관리자 레이아웃은 `<meta name="csrf-token">` 포함(HTMX CSRF 연동에 사용).
- **자산 로딩**: CSS/JS는 `</body>` 직전 `defer`. (참고: 레퍼런스는 첫 방문 시 인라인 critical CSS 후 CDN을 캐시 프리페치하는 최적화가 있으나, **초기 구현은 단순히 CDN 링크 로드로 충분**.)

### 15.2 토스트 메시지 시스템 (Flash → Toast) — 필수
레퍼런스는 플래시 메시지를 **우하단 고정 Bootstrap 토스트**로 렌더한다. 공통 프래그먼트 `fragments/message.html`로 구현.

| 종류 | 세션 키 | 헤더 배경 | 헤더 제목 | 아이콘 |
|------|---------|-----------|-----------|--------|
| 일반 | `message` | `bg-dark text-light` | **메시지** | info(원+i) SVG |
| 성공 | `success` | `bg-primary text-white` | **성공** | check(✓) SVG |
| 오류 | `error` | `bg-danger text-white` | **오류** | alert(원+!) SVG |

- **위치/동작**: `toast-container position-fixed bottom-0 end-0 p-3`, `z-index:1100`. `data-bs-autohide="false"`(자동 사라짐 없음) + 우상단 수동 닫기(`btn-close`). `role="alert"`, `aria-live="assertive"`.
- **HTMX 구현**: 액션 응답 시 토스트 영역을 **OOB swap**(`hx-swap-oob="true"`)으로 갱신하거나, 리다이렉트 시 `RedirectAttributes` 플래시 → 다음 페이지에서 렌더. Bootstrap JS의 `.toast('show')` 동등 동작.

### 15.3 레이아웃 구조 (4종)
- **`app`(공개)**: `.page > [navbar] > .page-wrapper > [slot] + [footer]`.
- **`auth`(인증/약관)**: `.page.page-center > .container.container-tight` — 중앙 정렬, 상단 로고, 카드.
- **`admin`(관리자)**: `body.layout-fluid > .page > [sidebar] > .page-wrapper > [topbar] + [slot] + [footer]`.
- **`error`**: 오류 전용(404/500/503 템플릿).
- 모든 레이아웃: `<head>`에 `seo`/`css` 슬롯, `</body>` 직전 `js` 슬롯.

### 15.4 공통 네비게이션 컴포넌트

**공개 navbar (`app_navbar`)**
- 상단 바: 좌측 로고(`branding/logo_color.svg` 있으면 이미지, 없으면 앱명 텍스트, `width:160`), 우측:
  - **비로그인** → "로그인" 링크(사람 아이콘).
  - **로그인** → 유저 드롭다운(이름/이메일 + 아바타). 메뉴: `대시보드`(admin만) · `프로필 수정` · `비밀번호 변경` · 구분선 · `로그아웃`.
- 하단 메뉴 바(collapse): 좌측 `홈`, 우측 `문의하기`(primary 버튼).

**관리자 sidebar (`admin_sidebar`)** — `data-bs-theme="dark"`, 세로 navbar
- 로고(`branding/logo_white.svg`, `max-width:120px`) → 대시보드 링크.
- 모바일용 상단 유저 드롭다운(아바타+이름/이메일): `프로필` · `비밀번호` · `로그아웃`.
- 메뉴(현재 URL 기준 `active` 하이라이트):
  - **대시보드** (홈 아이콘)
  - (role=admin 한정)
    - **사용자** (`/admin/user`)
    - **설정** ▾ (드롭다운): `브랜딩` · `버전 및 라이선스 정보` · `개인정보 처리방침` · `서비스 이용약관`
    - **시스템** ▾ (드롭다운): `데이터베이스`(**local 환경에서만 노출**) · `로그`(`target=_blank`) · `PhpInfo` · `백업`
  - **랜딩 페이지** (`/`, `target=_blank`)
- 설정/시스템 드롭다운은 현재 섹션이면 펼친 상태(`show`) 유지.

**공개 footer (`app_footer`)**
- 우측: `개인정보 처리방침` · `서비스 이용약관` 링크(둘 다 `target=_blank`).
- 좌측: `settings.footer` 문구(없으면 `© {연도} {앱명}`).

**아바타 규칙**(공통): 프로필 이미지 있으면 `user/profile_image/100/...` 사용, 없으면 이름/이메일 **첫 글자 이니셜** 또는 `/theme/no_profile_image.webp`.

### 15.5 페이지별 화면 명세

**랜딩 `/`** — `container-xl py-5`, 중앙 정렬: `display-5` 타이틀(앱명) + `fs-3` 서브카피 + `문의하기`(primary lg) 버튼. 전체 OG/twitter 메타 포함.

**통합 인증 `/signin`** — `auth` 레이아웃, 카드(`card-md`). `robots: noindex,nofollow`.
- 카드 타이틀이 모드별 동적: **로그인 / 회원가입 / 비밀번호 찾기**.
- 필드 동적 노출:
  - 이메일(항상)
  - 이름(회원가입만)
  - 인증 코드(재설정 + 코드 전송 후 + 미검증, placeholder "이메일로 받은 6자리 코드")
  - 비밀번호(로그인·회원가입, 재설정은 코드검증 후 "새 비밀번호") — **눈 아이콘 표시 토글**(`input-group-flat`).
- Turnstile 위젯(로그인·회원가입, `enabled()`일 때만).
- 제출 버튼 라벨 동적: `로그인` / `회원가입` / `인증 코드 전송` / `인증 코드 확인` / `비밀번호 재설정`.
- 하단 전환 링크: (로그인)"비밀번호 재설정 · 회원가입" / (회원가입)"로그인" / (재설정)"로그인".
- **local 한정** 하단: "로컬 개발 전용 퀵 로그인" + `클라이언트 로그인`·`관리자 로그인`(dark) 2버튼.

**문의 `/contact`** — `container-narrow`, 카드. 제목 "문의하기" + 안내문. 필드: 이름·이메일·제목(required 라벨) / 메시지(textarea rows=6) / 첨부(선택, hint "최대 10MB") / Turnstile. 제출 버튼은 **로딩 스피너 토글**("보내기" ↔ 스피너+"전송 중...").

**약관/개인정보 `/terms` `/privacy`** — 저장된 HTML(`th:utext`) 렌더.

**클라이언트/관리자 프로필** — 카드 "프로필 수정". 파일 업로드(`accept=image/jpg,jpeg,png`) + **선택 시 미리보기**(임시 URL). 현재 이미지 표시 + "이미지 삭제"(sm danger) 버튼. 저장 버튼 "이미지 저장".

**사용자 관리 `/admin/user`** — 핵심 인터랙티브 화면:
- 상단 행: 검색창(`input-icon` + 돋보기 SVG, **디바운스 500ms**, `type=search`) / 역할 필터 탭 `모든 역할(N)`·`사용자(N)`·`관리자(N)`(선택 시 `text-primary`, 카운트 실시간) / 우측 `새로운 사용자` 토글 버튼(열림 시 "닫기" + 위 화살표 아이콘).
- **생성 카드**(토글 펼침): 이메일+`중복 확인` 버튼(통과 시 "* 사용 가능") / 이름 / 비밀번호 / 역할 select(사용자·관리자) / 우측 `저장`.
- **수정 카드**(토글 펼침): 이메일(readonly, `bg-light`) / 이름 / 비밀번호(빈값=미변경) / 역할 / `닫기`·`수정`.
- **테이블**(`table-sm table-striped`): 컬럼 `ID · 이메일(아바타+주소) · 이름 · 역할 · 로그인 · (수정/삭제)`.
  - 역할 badge: **관리자 = `bg-blue-lt`**, **사용자 = `bg-purple-lt`**.
  - "로그인" = impersonate badge(`bg-dark`, 클릭 시 해당 유저 로그인).
  - 수정(연필, primary) / 삭제(휴지통, danger, **확인창** "정말 이 사용자를 삭제하시겠습니까?").
  - 브라우저 자동완성 방지용 hidden fake input 3종 포함.
- 하단 페이지네이션(`onEachSide(0)`).

**관리자 설정** — information(footer/version 폼) / privacy·terms(**HugeRTE** 에디터 + 저장) / branding(로고 8종 업로드 폼).

**관리자 대시보드 / 개발 도구** — §7 FR-7, FR-11 참조(요약 카드 / 백업 목록 테이블 / 시스템 정보 / 로그 뷰어).

### 15.6 공통 인터랙션 UI 패턴
| 패턴 | 레퍼런스 | Spring(HTMX) |
|------|----------|--------------|
| 로딩 버튼 | `wire:loading` 스피너 토글 | `hx-indicator` + Bootstrap 스피너 |
| 삭제 확인 | `wire:confirm="..."` | `hx-confirm="..."` |
| 비번 표시 토글 | `togglePasswordType` | 소형 JS 또는 HTMX |
| 검색 디바운스 | `wire:debounce.500ms` | `hx-trigger="keyup changed delay:500ms"` |
| 탭/카드 토글 | `wire:click`+상태 | `hx-get` 프래그먼트 교체 |
| 실시간 카운트/검증 | `wire:model.live` | `hx-trigger="change"` |
| Turnstile 리셋 | `dispatch('turnstile-reset')` | `HX-Trigger: turnstileReset` |

### 15.7 UI 문구 카탈로그 (한국어 — 그대로 유지)
- **토스트 헤더**: 메시지 / 성공 / 오류.
- **인증 성공/실패**: "이메일 또는 비밀번호가 올바르지 않습니다.", "스팸 방지 검증에 실패했습니다. 다시 시도해 주세요.", "비밀번호 재설정 코드를 이메일로 전송했습니다. 10분 이내에 입력해 주세요.", "코드가 확인되었습니다. 새 비밀번호를 설정해 주세요.", "유효하지 않거나 만료된 코드입니다.", "인증 코드가 만료되었습니다. 다시 요청해 주세요.", "비밀번호가 성공적으로 재설정되었습니다."
- **비밀번호 정책**: "비밀번호는 8~15자이며 대문자, 소문자, 숫자, 특수문자(!@#$%^&*)를 각각 1자 이상 포함해야 합니다."
- **레이트리밋**: "요청이 너무 많습니다. {N}초 후 다시 시도해 주세요." / "너무 많은 문의가 접수되었습니다. {N}초 후 다시 시도해 주세요."
- **사용자 관리**: "사용할 수 있는 이메일입니다." / "이메일이 확인되지 않았거나 이미 사용 중입니다." / "계정이 성공적으로 생성/업데이트/삭제되었습니다." / "정말 이 사용자를 삭제하시겠습니까?"
- **프로필**: "프로필 이미지가 업데이트/삭제되었습니다." / "이미지 크기는 2MB를 초과할 수 없습니다." / "이미지를 처리할 수 없습니다. JPEG 또는 PNG 파일인지 확인해 주세요."
- **문의**: "문의가 정상적으로 전송되었습니다. 빠른 시일 내에 답변 드리겠습니다." / "메일 전송에 실패했습니다. 잠시 후 다시 시도해 주세요."
- **설정**: "개인정보 처리방침/이용약관이 업데이트되었습니다." / "브랜딩 자산이 저장되었습니다."
- **버튼/라벨**: 로그인·회원가입·로그아웃·문의하기·보내기·전송 중...·저장·수정·삭제·닫기·중복 확인·이미지 저장·이미지 삭제·새로운 사용자·대시보드·사용자·설정·시스템·랜딩 페이지·프로필 수정·비밀번호 변경 등.

> **번역/문구는 신규 창작하지 말고 위 카탈로그를 그대로 사용**한다(레퍼런스 톤 유지).

### 15.8 화면 패리티 체크리스트 (수용 기준 보강)
- [ ] 우하단 토스트 3종(메시지/성공/오류)이 색상·아이콘·제목·수동닫기까지 동일.
- [ ] 레이아웃 4종, 공개 navbar/admin sidebar/footer 메뉴 구성·문구·active 동일.
- [ ] sidebar의 `데이터베이스`는 local에서만, `로그`는 새 탭으로 노출.
- [ ] signin 카드의 모드별 타이틀/필드/버튼 라벨/하단 링크 전환이 동일.
- [ ] 비밀번호 눈 아이콘 토글, 검색 500ms 디바운스, 삭제 확인창 동작.
- [ ] 사용자 테이블 컬럼·역할 badge 색상(admin=blue-lt, client=purple-lt)·impersonate·인라인 생성/수정 카드 동일.
- [ ] 로딩 버튼 스피너("전송 중...")·아바타 이니셜/기본이미지 처리 동일.
- [ ] 모든 UI 문구가 §15.7 카탈로그와 일치(한국어).

---

*본 PRD는 레퍼런스 보일러플레이트의 `docs/01-usage-guide.md`, `docs/02-setup.md`, `app/Livewire/*`, `routes/web.php`, `resources/views/**`에서 추출한 기능·화면 명세를 기준으로 작성되었다.*