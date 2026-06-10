# 브랜딩 자산 (로고 · 파비콘)

이 프로젝트는 브랜딩을 **관리자 패널 업로드 + 런타임 PNG 자동 생성**으로 처리합니다. (오프라인 CLI 변환 불필요)

> 핵심 클래스: `service/BrandingService.java`(파일 저장/조회), `service/ImageService.java`(Scrimage로 PNG 변환), `web/admin/AdminSettingController.java#saveBranding`(업로드 처리), `config/WebConfig.java`(`/branding/**` 서빙).

---

## 🗂 저장 위치 & 서빙

- 저장 디렉터리: **`storage/branding/`** (설정 키 `app.branding.dir`, 기본값 `storage/branding`).
- 웹 노출: **`/branding/**`** → `BrandingService.dir()` (WebConfig의 정적 핸들러). 즉 `storage/branding/logo_color.svg` → `https://.../branding/logo_color.svg`.

---

## ⬆️ 업로드 (관리자 패널)

**관리자 > 설정 > 브랜딩** (`/admin/setting/branding`)에서 업로드합니다. 받는 필드와 산출물:

| 업로드 필드 | 저장/생성 결과 |
|---|---|
| `logo_color_square_svg` | `logo_color_square.svg` |
| `logo_color_svg` | `logo_color.svg` (navbar 가로 로고) |
| `logo_white_square_svg` | `logo_white_square.svg` |
| `logo_white_svg` | `logo_white.svg` (어두운 배경용) |
| `logo_color_square_png` | **다중 사이즈 PNG** `logo_color_{512,192,180,150,32,16}.png` + **`favicon.ico`**(32px) 자동 생성 |
| `logo_color_png` | `logo_color.png` |
| `logo_white_square_png` | `logo_white_512.png` 자동 생성 |
| `logo_white_png` | `logo_white.png` |

- PNG 다중 사이즈/파비콘은 **정사각 PNG를 올리면 `ImageService.toPngSquare()`(Scrimage)가 런타임에 생성**합니다 — 별도 도구 불필요.
- 필요한 것만 올리면 됩니다(부분 업로드 허용). 실패 시 "브랜딩 자산을 저장하지 못했습니다" 토스트.

---

## 🎛 로고 vs 텍스트 폴백

브랜딩 파일이 **있으면 로고, 없으면 앱 이름 텍스트**가 나옵니다. `GlobalModelAttributes`가 존재 여부를 모델에 주입:

| 모델 키 | 판단 | 사용처 |
|---|---|---|
| `hasLogoColorSvg` | `BrandingService.exists("logo_color.svg")` | navbar (없으면 `${appName}` 텍스트) |
| `hasLogoWhiteSvg` | `exists("logo_white.svg")` | 관리자 사이드바 등 |

템플릿 예 (`fragments/app_navbar.html`):
```html
<img th:if="${hasLogoColorSvg}" th:src="@{/branding/logo_color.svg}" th:alt="${appName}" width="160">
<span th:unless="${hasLogoColorSvg}" class="fw-bold fs-3" th:text="${appName}">BP</span>
```

---

## 🔖 파비콘 우선순위 (주의)

- 기본 파비콘: **정적** `static/favicon.ico` → `/favicon.ico` (레이아웃 `<link rel="icon" href="/favicon.ico">`).
- 브랜딩 업로드 시 생성되는 `storage/branding/favicon.ico`는 **`/branding/favicon.ico`** 로 서빙됩니다(경로가 다름).
- 즉 둘은 **다른 URL**입니다. 브랜딩 파비콘을 실제로 쓰려면 레이아웃의 `<link rel="icon">`을 `/branding/favicon.ico`로 바꾸거나, 정적 `static/favicon.ico`를 교체하세요.

---

## ✏️ 새 프로젝트 브랜딩으로 교체하는 방법

**방법 A — 관리자 패널 (권장)**
1. `/admin/setting/branding` 접속 → 컬러/화이트 로고(SVG)와 정사각 PNG 업로드.
2. 저장하면 `storage/branding/`에 파일 생성 + PNG 다중 사이즈/파비콘 자동 생성.
3. navbar/사이드바가 자동으로 로고로 전환(`exists()` 폴백).

**방법 B — 파일 직접 배치**
- `storage/branding/`에 `logo_color.svg`, `logo_white.svg` 등을 직접 넣어도 됩니다(업로드와 동일 효과). PNG 다중 사이즈가 필요하면 직접 만들어 넣거나 패널로 한 번 올리세요.

> `storage/`는 보통 `.gitignore` 대상(런타임 산출물)입니다. 브랜딩을 리포지토리에 포함하려면 별도 관리하세요.

---

## ✅ 체크리스트

- [ ] 컬러 로고(`logo_color.svg`) — navbar용 가로 로고
- [ ] 화이트 로고(`logo_white.svg`) — 어두운 배경/관리자용
- [ ] 정사각 컬러 PNG 업로드 → 다중 사이즈 + 파비콘 자동 생성 확인
- [ ] 파비콘을 브랜딩 것으로 쓸지(`/branding/favicon.ico`) vs 정적(`/favicon.ico`) 결정
- [ ] 로고 미업로드 시 `${appName}` 텍스트로 잘 보이는지 확인
