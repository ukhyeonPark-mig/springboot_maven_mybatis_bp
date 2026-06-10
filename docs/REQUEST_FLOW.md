# 요청 흐름 & 보안 점검 맵 (BP 보일러플레이트)

> Spring Boot + MyBatis(XML) + Thymeleaf(Layout Dialect) + HTMX + Spring Session(JDBC).
> 라우트 경로/컨트롤러 줄번호는 확인된 값. 서비스/매퍼 내부 줄번호는 점검 시 대조용 참고치.

---

## 1. 요청 처리 파이프라인 (큰 그림)

```
HTTP 요청
 → Servlet 필터: Spring Security FilterChain
     - CsrfFilter (POST/PUT/DELETE에 CSRF 토큰 검증)
     - SecurityContextHolderFilter (Spring Session JDBC에서 SecurityContext 복원)
     - LogoutFilter (GET /logout, GET /admin/logout 매칭)
     - 인가: authorizeHttpRequests 규칙 평가 → permitAll / authenticated / hasRole('ADMIN')
        · 미인증 + 보호자원 → formLogin 진입점이 /signin 으로 리다이렉트 (원래 URL 세션 저장)
        · 인증됨 + /admin/** 인데 admin 아님 → 403 (templates/error/403.html)
 → DispatcherServlet → @Controller 메서드
     - @ControllerAdvice(GlobalModelAttributes)가 모든 뷰 모델에 공통값 주입
     - 컨트롤러 → Service → Mapper(인터페이스) → MyBatis XML → MySQL
 → 뷰: Thymeleaf (전체 페이지 layout:decorate, 또는 HTMX fragment 반환)
```

핵심 설계 2가지:
- **수동 로그인**: 실제 인증은 Spring `formLogin`이 아니라 `SigninController`가 직접 처리(rate-limit + Turnstile + HTMX fragment). `formLogin`은 "미인증 시 /signin으로 보내고 원래 URL 저장"하는 **진입점 역할만**.
- **HTMX fragment 응답**: 대부분의 POST는 전체 페이지가 아니라 `템플릿 :: fragment`를 부분 갱신으로 반환. 리다이렉트가 필요하면 `HX-Redirect` 헤더 + `fragments/empty :: empty` 반환.

---

## 2. 보안 / 인증 서브시스템

| 구성요소 | 파일 | 역할 |
|---|---|---|
| 인가 규칙·로그인 진입점·로그아웃·BCrypt | `config/SecurityConfig.java` | FilterChain 정의 (아래 표) |
| 수동 인증·세션 수립·타깃 해석·세션 갱신 | `security/AuthSessionService.java` | `authenticate()`, `establishAndResolveTarget()`, `tokenFor()`, `refresh()` |
| UserDetails 구현(+표시용 헬퍼) | `security/SecurityPrincipal.java` | `from(User)`, `getDisplayName()`, `getInitial()`, `isAdmin()`, `getAuthorities()` |
| 이메일로 사용자 로드 | `security/UserDetailsServiceImpl.java` | `loadUserByUsername(email)` → `UserMapper.findByEmail` |
| 역할 enum | `domain/Role.java` | `authority()` = `ROLE_` + 대문자 |
| rate limit | `support/RateLimiterService.java` | Bucket4j 고정창, (action+IP)별 |
| 비밀번호 정책 | `support/PasswordPolicy.java` | 8~15자, 대/소/숫자/특수 정규식 |
| OTP | `service/OtpService.java` | 6자리 생성, 10분 TTL, 5회 제한, 상수시간 비교 |
| 캡차 | `service/TurnstileService.java` | Cloudflare Turnstile, local=통과, prod+secret없음=fail-open |

### SecurityConfig 인가 규칙 (`config/SecurityConfig.java:45-62`)
| 패턴 | 등급 |
|---|---|
| `/`, `/signin`, `/signin/**`, `/contact`, `/privacy`, `/terms`, `/sitemap.xml` | permitAll |
| `/css/**`, `/js/**`, `/image/**`, `/branding/**`, `/theme/**`, `/favicon.ico`, `/error` | permitAll |
| `/admin/**` | hasRole('ADMIN') |
| `/client/**` | authenticated |
| 그 외 모든 요청 | authenticated |

- 로그인 진입점: `.formLogin().loginPage("/signin")` (실제 검증 안 함, 진입점 전용)
- 로그아웃: **GET** `/logout` 또는 `/admin/logout` → 세션 무효화 + `SESSION` 쿠키 삭제 → `/`로
- 비밀번호 인코더: `BCryptPasswordEncoder(12)`

### 로그인 흐름 (수동)
```
POST /signin/login (SigninController)
 1) RateLimiterService.attempt("signin:"+ip, 5, 60s)         // 초과 시 차단 메시지
 2) TurnstileService.verify(token, ip)                        // 실패 시 HX-Trigger: turnstileReset
 3) AuthSessionService.authenticate(email, pwd)
       → AuthenticationManager → UserDetailsServiceImpl.loadUserByUsername
       → UserMapper.findByEmail → SecurityPrincipal.from(user)
       → BCrypt matches (실패 시 AuthenticationException)
 4) AuthSessionService.establishAndResolveTarget(auth, req, resp)
       → 저장된 원래 URL(HttpSessionRequestCache) 있으면 그곳, 없으면 admin→/admin/dashboard, 그 외→/
       → SecurityContext 저장(HttpSession), request.changeSessionId() (세션 고정 방지)
 5) 응답: HX-Redirect 헤더 + fragments/empty :: empty
```
> 점검 포인트: 세션 ID 회전(`changeSessionId`), rate-limit 키/임계값, Turnstile fail-open 정책(prod에서 secret 비면 통과), 로그아웃이 GET이라는 점(CSRF 영향).

---

## 3. 횡단 관심사 (모든 요청 공통)

### GlobalModelAttributes (`web/GlobalModelAttributes.java`, `@ControllerAdvice`)
모든 뷰 모델에 주입 — 점검 시 "이 값이 어디서 오나" 추적용:
| 모델 키 | 출처 |
|---|---|
| `appName` | `AppProperties.name()` (`app.name`) |
| `setting` | `SettingService.get()` → `SettingMapper.findFirst` (없으면 insert) |
| `currentUri`, `currentUrl` | `HttpServletRequest` |
| `r2PublicUrl` | `R2StorageService.publicBaseUrl()` (R2 도메인 또는 로컬 `/storage`) |
| `hasLogoColorSvg`, `hasLogoWhiteSvg` | `BrandingService.exists(...)` |
| `isLocal` | `Environment.matchesProfiles("local")` |
| `message`/`success`/`error` | 세션 1회성 플래시(`FlashMessage`) 소비 → 토스트 |
| `_csrf` | 요청의 `CsrfToken` → 레이아웃 meta 태그 → HTMX 헤더 |

### 기타 설정
| 파일 | 역할 |
|---|---|
| `config/WebConfig.java` | 정적 핸들러: `/branding/**`→`BrandingService.dir()`, `/storage/**`→로컬 업로드(R2 미사용 시) |
| `config/MyBatisConfig.java` | `@MapperScan("com.example.bp.mapper")` |
| `config/ThymeleafConfig.java` | LayoutDialect 빈 (`layout:decorate`/`layout:fragment`) |
| `config/AsyncConfig.java` | `@EnableAsync` (현재 메일은 동기 전송) |
| `config/SchedulingConfig.java` | `@EnableScheduling` + `@Profile("prod")`: 매일 02:00 백업, 02:30 14일 경과분 정리 |

---

## 4. 라우트 맵 (영역별)

### 4.1 공개 (permitAll)
| 메서드 · 경로 | 컨트롤러#메서드 (파일) | 흐름 | 응답 |
|---|---|---|---|
| GET `/` | HomeController#index (`web/home/HomeController.java:10`) | 서비스 없음 | 전체 `home/index` |
| GET `/privacy` | LegalController#privacy (`web/home/LegalController.java:14`) | `setting.privacy`(GlobalModelAttributes) | 전체 `home/privacy` (`th:utext`) |
| GET `/terms` | LegalController#terms (`web/home/LegalController.java:19`) | `setting.terms` | 전체 `home/terms` |
| GET `/contact` | ContactController#contact (`web/home/ContactController.java:45`) | Turnstile enabled/siteKey 주입 | 전체 `home/contact` |
| POST `/contact` | ContactController#send (`:51`) | RateLimit(contact 3/600s) → Turnstile.verify → 검증 → MailService.sendHtml(reply-to+첨부) | HTMX `home/contact :: card` |
| GET `/sitemap.xml` | SitemapController#sitemap (`web/home/SitemapController.java:22`) | `AppProperties.url()` 기반 XML 생성 | XML 문자열 |

### 4.2 인증 (signin, permitAll) — `web/home/SigninController.java`
| 메서드 · 경로 | 메서드(줄) | 흐름 | 응답 |
|---|---|---|---|
| GET `/signin` | signin (`:74`) | OTP 세션 정리 | 전체 `auth/signin` |
| GET `/signin/tab` | tab (`:81`) | 탭 전환 | HTMX `auth/signin :: card` |
| POST `/signin/login` | login (`:89`) | RateLimit 5/60s → Turnstile → authenticate → establish | HTMX card / HX-Redirect |
| POST `/signin/signup` | signup (`:119`) | 검증 → RateLimit 3/300s → Turnstile → `UserService.create` → 로그인 | HTMX card / HX-Redirect |
| POST `/signin/reset/send` | resetSend (`:151`) | RateLimit 3/300s → `UserMapper.updateOtp` → `MailService` OTP 메일 | HTMX card |
| POST `/signin/reset/verify` | resetVerify (`:186`) | RateLimit 10/600s → 만료/시도/상수시간 비교 → 실패 시 `incrementOtpAttempts` | HTMX card |
| POST `/signin/reset/password` | resetPassword (`:236`) | 세션 검증된 이메일 확인 → PasswordPolicy → `updatePasswordAndVerify` → 로그인 | HTMX card / HX-Redirect |
| POST `/signin/quick` | quickLogin (`:267`) | **local 전용** `findFirstByRole` → 로그인 | HTMX / HX-Redirect |

### 4.3 클라이언트 (authenticated, `/client/**`)
| 메서드 · 경로 | 컨트롤러#메서드 (파일) | 흐름 (서비스 → 매퍼 XML id) | 응답 |
|---|---|---|---|
| GET `/client/profile` | ClientProfileController#profile (`web/client/ClientProfileController.java:34`) | 프로필 이미지 URL = `r2PublicUrl + /user/profile_image/100/ + 파일명` | 전체 `client/profile` |
| POST `/client/profile` | #save (`:39`) | `ProfileImageService.validate`(≤2MB, jpg/png) → WebP 100·400 변환(`ImageService`) → `R2StorageService.putPublic` ×2 → `UserMapper.updateProfileImage` → 옛 이미지 삭제 → `AuthSessionService.refresh` | HTMX `client/profile :: card` |
| POST `/client/profile/delete` | #delete (`:58`) | `R2StorageService.deletePublic` ×2 → `updateProfileImage(null)` → `refresh` | HTMX card |
| GET `/client/password` | ClientPasswordController#password (`web/client/ClientPasswordController.java:28`) | — | 전체 `client/password` |
| POST `/client/password` | #update (`:33`) | `findById` → `checkPassword`(BCrypt) → PasswordPolicy → 확인일치 → `UserMapper.updatePassword` | HTMX `client/password :: card` |

### 4.4 관리자 (hasRole ADMIN, `/admin/**`)
| 메서드 · 경로 | 컨트롤러#메서드 (파일:줄) | 흐름 (매퍼 XML id) | 응답 |
|---|---|---|---|
| GET `/admin/dashboard` | AdminDashboardController#dashboard (`AdminDashboardController.java:10`) | 플레이스홀더 | 전체 `admin/dashboard` |
| GET `/admin/user` | AdminUserController#index (`AdminUserController.java:37`) | `search`/`countSearch`/`count`/`countByRole` | 전체 `admin/user` |
| GET `/admin/user/panel` | #panelFragment (`:46`) | 동일(검색/필터/페이지) | HTMX `admin/user :: panel` |
| POST `/admin/user/check-email` | #checkEmail (`:55`) | `existsByEmail` → 세션 플래그 | HTMX `:: emailFeedback` |
| POST `/admin/user/create` | #create (`:67`) | 세션 이메일확인 → PasswordPolicy → `UserService.create`→`insert` | HTMX `:: panel` |
| GET `/admin/user/edit/{id}` | #editOpen (`:101`) | `findById` | HTMX `:: panel`(edit) |
| POST `/admin/user/update` | #update (`:117`) | `updateNameRole`(+선택 `updatePassword`) | HTMX `:: panel` |
| POST `/admin/user/delete/{id}` | #delete (`:143`) | `deleteById` | HTMX `:: panel` |
| POST `/admin/user/impersonate/{id}` | #impersonate (`:154`) | `findById` → `AuthSessionService.refresh`(대상 사용자로 전환) → HX-Redirect `/` | HTMX empty |
| GET `/admin/account/profile` | AdminAccountController#profile (`AdminAccountController.java:38`) | — | 전체 `admin/account/profile` |
| POST `/admin/account/profile` | #saveProfile (`:43`) | 클라이언트 프로필과 동일 파이프라인 | HTMX `:: card` |
| POST `/admin/account/profile/delete` | #deleteProfile (`:62`) | 이미지 삭제 + refresh | HTMX `:: card` |
| GET `/admin/account/password` | #password (`:74`) | — | 전체 `admin/account/password` |
| POST `/admin/account/password` | #updatePassword (`:79`) | `findById`→`checkPassword`→정책→`updatePassword` | HTMX `:: card` |
| GET `/admin/setting/information` | AdminSettingController#information (`AdminSettingController.java:36`) | `findFirst` (footer/version) | 전체 |
| POST `/admin/setting/information` | #saveInformation (`:42`) | `updateInformation`→`update` (@Transactional) | HTMX `:: card` |
| GET `/admin/setting/privacy` | #privacy (`:52`) | `findFirst` | 전체 (HugeRTE 에디터) |
| POST `/admin/setting/privacy` | #savePrivacy (`:58`) | `updatePrivacy`→`update` | HTMX `fragments/message :: toasts` |
| GET `/admin/setting/terms` | #terms (`:66`) | `findFirst` | 전체 |
| POST `/admin/setting/terms` | #saveTerms (`:72`) | `updateTerms`→`update` | HTMX toasts |
| GET `/admin/setting/branding` | #branding (`:80`) | — | 전체 |
| POST `/admin/setting/branding` | #saveBranding (`:85`) | SVG 4종 저장 + PNG 다중사이즈(512/192/180/150/32/16)+favicon 생성 (`BrandingService`/`ImageService`) | HTMX `:: card` |
| GET `/admin/development/php` | AdminSystemController#system (`AdminSystemController.java:27`) | 런타임/JVM/Spring 정보 수집(읽기) | 전체 `admin/development/system` (메뉴명 SystemInfo) |
| GET `/admin/development/logs` | AdminLogsController#logs (`AdminLogsController.java:27`) | `logging.file.name` tail 500줄 | 전체 |
| GET `/admin/development/backup` | AdminBackupController#backup (`AdminBackupController.java:31`) | `BackupService.list()` | 전체 |
| POST `/admin/development/backup/create` | #create (`:37`) | `mysqldump`(ProcessBuilder, MYSQL_PWD) → zip | HTMX `:: card` |
| POST `/admin/development/backup/delete/{name}` | #delete (`:49`) | 경로 traversal 방지 후 삭제 | HTMX `:: card` |
| GET `/admin/development/backup/download/{name}` | #download (`:61`) | `resolve(name)` 안전 검증 → 파일 다운로드 | 바이너리 |
| GET `/admin/development/database` | AdminDatabaseController#database (`AdminDatabaseController.java:30`) | **`@Profile("local")` 추가 게이트** · `SHOW TABLES` + 화이트리스트 검증 후 `SELECT ... LIMIT 100` | 전체 (읽기 전용) |

> 점검 포인트(관리자): ① `/admin/development/database`·`/php`의 local 게이팅 확인, ② 백업의 `mysqldump` 외부 프로세스/비번 전달(`MYSQL_PWD`) 안전성, ③ backup name 경로 traversal 방지, ④ DB 브라우저의 테이블명 화이트리스트(SQL injection 방지), ⑤ impersonate 권한·로깅.

---

## 5. 서비스 ↔ 매퍼 ↔ XML 레퍼런스

### Mapper 인터페이스 / XML
- `mapper/UserMapper.java` ↔ `mappers/UserMapper.xml`
  - 조회: `findByEmail`, `findById`, `findFirstByRole`, `existsByEmail`, `count`, `search`, `countSearch`, `countByRole`
  - 변경: `insert`, `updateOtp`, `incrementOtpAttempts`, `invalidateOtp`, `updatePasswordAndVerify`, `updatePassword`, `updateProfileImage`, `updateNameRole`, `deleteById`
- `mapper/SettingMapper.java` ↔ `mappers/SettingMapper.xml`
  - `findFirst`, `insert`, `update`

### 주요 서비스
| 서비스 | 책임 |
|---|---|
| `service/UserService.java` | 사용자 CRUD, BCrypt 인코딩, OTP, 검색/페이지네이션 |
| `service/SettingService.java` | settings 싱글톤 get/update (없으면 생성) |
| `service/OtpService.java` | OTP 생성·만료·시도·상수시간 비교 |
| `service/TurnstileService.java` | Cloudflare 검증(enabled/verify) |
| `service/MailService.java` | SES SMTP HTML 메일(미설정 시 로그 폴백), 첨부/reply-to |
| `service/ProfileImageService.java` | 검증→WebP 변환→R2 업로드→옛 이미지 정리 |
| `service/ImageService.java` | Scrimage: WebP cover, PNG square |
| `service/R2StorageService.java` | R2(S3 호환) 또는 로컬 `/storage` 추상화 |
| `service/BrandingService.java` | `storage/branding/` 로고/파비콘 저장·조회 |
| `service/BackupService.java` | mysqldump 백업 생성/목록/삭제/다운로드(경로 안전) |
| `security/AuthSessionService.java` | 수동 인증·세션 수립·세션 갱신(refresh) |

---

## 6. 템플릿 구조

```
layouts/auth.html    → 로그인/법적 페이지 (CSRF meta, HTMX, htmx_csrf 스크립트)
layouts/app.html     → 공개/클라이언트 (navbar/footer, 프로필 아바타)
layouts/admin.html   → 관리자 (admin_sidebar/topbar/footer)
layouts/error.html   → 오류 페이지

공통 fragments:
  fragments/htmx_csrf.html  → htmx:configRequest 시 CSRF 헤더 자동 첨부
  fragments/message.html    → toasts (message/success/error)
  fragments/empty.html      → HX-Redirect 전용 빈 본문
  fragments/app_navbar.html → 사용자 메뉴 아바타(없으면 /image/no_profile_image.webp)
  fragments/admin_sidebar.html / admin_topbar.html / admin_footer.html

오류: error/403.html (admin 아님), 기타 error/*
```

---

## 7. 보안 점검 체크리스트 (직접 확인용)
- [ ] CSRF: 모든 상태변경 POST가 `_csrf` 헤더/필드를 보내는가? 로그아웃이 GET인 점은 의도된 설계인가?
- [ ] 인가 매트릭스: `/client/**` authenticated, `/admin/**` ROLE_ADMIN — 우회 경로 없는가? (정적 `/image/**`,`/storage/**` 노출 범위)
- [ ] 세션: 로그인 시 `changeSessionId()` 호출(세션 고정 방지), 로그아웃 시 `SESSION` 쿠키 삭제 + 무효화.
- [ ] rate-limit: signin 5/60s, signup·reset-send 3/300s, reset-verify 10/600s, contact 3/600s — IP 산출 방식(프록시 뒤 `forward-headers-strategy`).
- [ ] OTP: 10분 TTL, 5회 제한, 상수시간 비교, 검증 후 무효화.
- [ ] Turnstile fail-open: prod에서 secret 미설정 시 통과 — 운영 정책상 허용 가능한가?
- [ ] 파일 업로드: 프로필 ≤2MB·MIME 화이트리스트, 첨부 ≤10MB. WebP 변환으로 원본 메타데이터 제거.
- [ ] 관리자 dev 도구: DB 브라우저/SystemInfo는 local 전용 게이트, 백업 name 경로 traversal 방지, mysqldump 자격증명 전달.
- [ ] 비밀번호: BCrypt cost 12, 정책 정규식 8~15자.
- [ ] 시드 관리자(`admin@example.com`/`password1!`) — 운영 배포 전 변경.
