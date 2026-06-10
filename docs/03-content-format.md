# settings.terms / settings.privacy — HTML 콘텐츠 포맷 규약

`settings` 테이블의 `terms`(서비스 이용약관)와 `privacy`(개인정보 처리방침) 컬럼은 **HTML 문자열**로 저장되며, 고정된 HTML 포맷을 따릅니다.

이 규약을 지켜야 `/terms`, `/privacy` 페이지에서 Tabler 스타일링이 올바르게 적용됩니다. 관리자 패널의 **`/admin/setting/terms`, `/admin/setting/privacy`** 는 HugeRTE 에디터로 동일 포맷 편집을 지원합니다.

> 저장: `AdminSettingController#saveTerms`/`#savePrivacy` → `SettingService.updateTerms/updatePrivacy` → `SettingMapper.update`
> 출력: `LegalController`(`/terms`, `/privacy`)에서 `th:utext`로 렌더 (이스케이프 안 함 — 신뢰된 관리자 입력 전제)

---

## 📐 허용 태그와 클래스

| 용도 | 태그 | 필수 클래스 |
|------|------|-----------|
| 본문 단락 | `<p>` | `p1`, `p2`, `p3` (시각적 간격만 다름) |
| 강조 (인라인) | `<strong>` | — |
| 줄바꿈 | `<br>` | — |
| 링크 | `<a href="...">` | — |
| 순서 없는 목록 | `<ul><li><p class="p1">...</p></li></ul>` | 항목 안쪽은 `<p class="p1">` |
| 섹션 강조 | `<span class="s1"><strong>...</strong></span>` | 날짜·라벨용 |

- `p1`/`p2`/`p3` — 상하 마진만 다른 단락 클래스
- `s1` — 중요한 라벨/헤더(시행일자 등)에 `<strong>`과 조합
- HTML entity 허용: `&ldquo;`, `&rdquo;`, `&nbsp;` 등

---

## 🧱 구조 규칙

1. **첫 블록**에 시행일자/최종수정일:
   ```html
   <p class="p1"><span class="s1"><strong>시행일자:</strong></span> 2025년 1월 1일<br>
   <span class="s1"><strong>최종 수정일:</strong></span> 2025년 1월 1일</p>
   ```
2. **각 조항**은 `<p class="p3">`로 시작, 숫자+제목+`<br>` 뒤에 본문:
   ```html
   <p class="p3"><strong>1. 서문<br></strong>본문 내용...</p>
   ```
3. **불릿 목록**은 `<li>` 안에 `<p class="p1">`:
   ```html
   <ul>
       <li><p class="p1">첫 번째 항목</p></li>
       <li><p class="p1">두 번째 항목</p></li>
   </ul>
   ```
4. **문의처**는 마지막 조항으로, 회사명/이메일/주소 순:
   ```html
   <p class="p3"><strong>11. 문의처<br></strong>관련 문의는 아래로 연락 주시기 바랍니다:</p>
   <p class="p1">주식회사 예시<br>
   이메일: <a href="mailto:hello@example.com">hello@example.com</a><br>
   주소: ...</p>
   ```

---

## 📄 예시 (축약) — `terms`

```html
<p class="p1"><span class="s1"><strong>시행일자:</strong></span> 2025년 1월 1일<br><span class="s1"><strong>최종 수정일:</strong></span> 2025년 1월 1일</p>

<p class="p3"><strong>1. 서문<br></strong>본 이용약관은 ...에서 제공되는 모든 서비스의 이용에 적용됩니다. ...</p>

<p class="p3"><strong>2. 제공 서비스<br></strong>당사는 다음 제품 및 서비스를 제공합니다:</p>
<ul>
    <li><p class="p1">소스코드 다운로드</p></li>
    <li><p class="p1">설치/커스터마이징 서비스</p></li>
</ul>

<p class="p3"><strong>11. 문의처<br></strong>약관 관련 문의는 아래로 연락해주시기 바랍니다:</p>
<p class="p1">주식회사 예시<br>
이메일: <a href="mailto:hello@example.com">hello@example.com</a><br>
주소: ...</p>
```

`privacy`도 동일 구조(서문 → 수집항목 → 목적 → 제공/공유 → 보유기간 → 보안 → 이용자 권리 → … → 문의처)로 작성합니다.

> 초기 시드에는 약관/개인정보가 비어 있을 수 있습니다. 관리자 패널에서 위 포맷으로 입력하세요. (또는 시드 마이그레이션에 넣어도 됩니다.)

---

## ✅ 체크리스트 (신규 프로젝트 시)

- [ ] 회사명/도메인/이메일/주소를 실제 값으로 치환
- [ ] 시행일자·최종 수정일 업데이트
- [ ] 제공 서비스·수집 정보 항목을 실제 서비스에 맞게 갱신
- [ ] `p1/p2/p3`, `s1`, `<li><p class="p1">` 패턴 준수 (스타일 깨짐 방지)
- [ ] 전체 조항을 **법무 검토** 후 배포
