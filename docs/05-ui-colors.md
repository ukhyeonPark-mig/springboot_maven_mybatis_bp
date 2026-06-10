# UI 색상 사용 규칙 (Tabler) — 대비/가독성

> 출처: [Tabler 색상](https://docs.tabler.io/ui/base/colors) · [Tabler 버튼](https://docs.tabler.io/ui/components/buttons) (이 프로젝트는 `@tabler/core@1.4.0`)

버튼/뱃지에서 **배경색과 텍스트색이 부조화하여 글자가 안 보이는** 문제를 막기 위한 규칙입니다. **AI에게 화면 작업을 시킬 때 이 문서를 함께 제공**하면 색 오용을 방지할 수 있습니다.

---

## 🚦 핵심 규칙 (이것만 지켜도 90% 해결)

1. **버튼은 "시맨틱 클래스"만 쓴다.** `btn-primary`, `btn-secondary`, `btn-success`, `btn-danger`, `btn-warning`, `btn-info`, `btn-dark`, `btn-light` — 이들은 Tabler가 **텍스트색을 자동으로 맞춰** 둡니다. 안전합니다.
2. **컬러 뱃지/라벨은 `-lt`(soft) 변형을 쓴다.** `badge bg-blue-lt`, `badge bg-green-lt` 처럼. `-lt`는 **연한 배경 + 진한 같은 계열 글자**로 디자인돼 대비가 항상 좋습니다.
3. **밝은 배경색에 흰 글자 금지.** `yellow`, `lime`, `orange`, `light` 계열 **솔리드 배경**엔 **어두운 글자**(`text-dark`)를 써야 합니다.
4. **`bg-색 + text-색`을 손으로 조합하지 않는다.** 꼭 필요하면 아래 명도 표를 보고 맞추되, 가능하면 1·2번(시맨틱/`-lt`)으로 우회하세요.
5. **확신이 없으면** → 버튼은 `btn-primary`/`btn-ghost-secondary`/`btn-outline-*`, 뱃지는 `bg-*-lt`. 이게 기본값입니다.

---

## 🎨 색상 팔레트 & 명도 (솔리드 배경 기준)

Tabler 기본 색: `blue` `azure` `indigo` `purple` `pink` `red` `orange` `yellow` `lime` `green` `teal` `cyan` (+ `gray` 스케일).
유틸리티: `bg-{색}`(솔리드 배경) · `text-{색}`(글자색) · `bg-{색}-lt`(연한 배경+진한 글자).

| 솔리드 배경 색 | 명도 | 솔리드 위 글자색 |
|---|---|---|
| `blue` `azure` `indigo` `purple` `pink` `red` `green` `teal` `cyan` `dark` | **어두움** | **흰색** (`text-white`) — 보통 자동 |
| `yellow` `lime` `orange` `light` | **밝음** ⚠️ | **어두운색** (`text-dark`) — 흰 글자면 안 보임 |
| `gray-50` ~ `gray-300` | 밝음 | 어두운색 |
| `gray-600` ~ `gray-900` | 어두움 | 흰색 |

> **`-lt` 변형(`bg-blue-lt` 등)은 명도 걱정이 없습니다** — 항상 연한 배경 + 같은 계열 진한 글자라 자동으로 잘 보입니다. **컬러를 쓰고 싶으면 `-lt`가 1순위.**

---

## 🔘 버튼

```html
<!-- ✅ 안전: 시맨틱 (텍스트색 자동) -->
<button class="btn btn-primary">기본 동작</button>     <!-- 파랑 + 흰 글자 -->
<button class="btn btn-success">저장</button>          <!-- 초록 + 흰 글자 -->
<button class="btn btn-danger">삭제</button>           <!-- 빨강 + 흰 글자 -->
<button class="btn btn-warning">주의</button>          <!-- 황색 + 어두운 글자(자동) -->
<button class="btn btn-secondary">취소</button>        <!-- 회색 -->

<!-- ✅ 안전: 아웃라인 / 고스트 (배경 거의 없음 → 대비 문제 없음) -->
<button class="btn btn-outline-primary">보조</button>
<button class="btn btn-ghost-secondary">덜 강조</button>

<!-- ❌ 위험: 밝은 명명색 솔리드 + 기본 글자 → 글자 안 보일 수 있음 -->
<button class="btn btn-yellow">노랑</button>           <!-- 피하거나 text-dark 명시 -->
<button class="btn btn-lime">라임</button>
```

규칙:
- **시맨틱(`btn-primary/success/danger/warning/info/secondary/dark/light`) 우선.**
- 강조를 낮추려면 `btn-outline-*` 또는 `btn-ghost-*`.
- `btn-yellow`/`btn-lime`/`btn-orange` 같은 **밝은 명명색 솔리드는 지양**. 꼭 쓰면 `class="btn btn-yellow text-dark"`.
- 크기: `btn-sm`, `btn-lg` 등. 아이콘 전용: `btn-icon`.

---

## 🏷 뱃지 / 라벨 / 상태

```html
<!-- ✅ 권장: -lt 소프트 변형 (대비 항상 좋음) -->
<span class="badge bg-blue-lt">정보</span>
<span class="badge bg-green-lt">성공 · 활성</span>
<span class="badge bg-yellow-lt">대기</span>     <!-- 밝은 색도 -lt면 안전 -->
<span class="badge bg-red-lt">위험 · 실패</span>

<!-- ⚠️ 솔리드 뱃지: 어두운 색만 (흰 글자 자동). 밝은 색은 피할 것 -->
<span class="badge bg-blue">파랑</span>          <!-- OK -->
<span class="badge bg-yellow">노랑</span>        <!-- ❌ 글자 안 보일 위험 → bg-yellow-lt 사용 -->
```

- **컬러 뱃지는 무조건 `bg-{색}-lt`** 를 기본으로 (이 프로젝트도 navbar 아바타 등에서 `bg-blue-lt` 사용).
- 의미 매핑 권장: 성공/활성=`green`, 대기/주의=`yellow`, 위험/실패=`red`, 정보=`blue`/`azure`, 중립=`secondary`/`gray`.

---

## 🧩 배경 위에 직접 텍스트를 얹을 때

카드/배너 등에서 `bg-{색}`을 직접 쓰면, **위 명도 표대로 글자색을 명시**하세요.

```html
<div class="bg-red text-white p-3">어두운 배경 → 흰 글자</div>     <!-- ✅ -->
<div class="bg-yellow text-dark p-3">밝은 배경 → 어두운 글자</div>  <!-- ✅ -->
<div class="bg-yellow text-white p-3">밝은 배경 + 흰 글자</div>     <!-- ❌ 안 보임 -->

<!-- 더 안전: -lt 변형은 글자색을 안 줘도 알아서 대비됨 -->
<div class="bg-yellow-lt p-3">연한 배경 + 진한 글자 (자동)</div>   <!-- ✅ -->
```

---

## ✅ 빠른 치트시트 (AI에게 줄 한 줄 요약)

> **버튼은 `btn-primary/success/danger/warning/secondary` 등 시맨틱만. 컬러 뱃지·배경은 `bg-{색}-lt`(soft) 사용. 밝은 솔리드색(yellow/lime/orange/light)에 흰 글자 금지 — 쓰려면 `text-dark`. 확신 없으면 `btn-primary` / `bg-*-lt`.**

---

## 🔎 점검(리뷰) 체크리스트

- [ ] 밝은 배경(`yellow`/`lime`/`orange`/`light`/`*-lt`)에 `text-white`를 쓰지 않았는가?
- [ ] 컬러 뱃지에 솔리드 `bg-{색}`(특히 밝은 색) 대신 `bg-{색}-lt`를 썼는가?
- [ ] 버튼이 시맨틱 클래스(또는 outline/ghost)인가? 명명 밝은색 솔리드(`btn-yellow` 등)를 남발하지 않았는가?
- [ ] `bg-{색}`을 직접 쓴 곳에 명도에 맞는 글자색을 명시했는가?
