---
name: decision-log
description: 새 Architecture Decision Record를 docs/adr/ 하위에 생성한다. /decision-log <kebab-case-title> 로 호출 (예 /decision-log use-r2dbc-for-persistence).
---

# decision-log

ripple-chat의 주요 의사결정을 ADR로 기록.

## 절차

1. 인자로 kebab-case 제목을 받는다. 없으면 사용자에게 묻는다.
2. 다음 순번 계산
   - `ls docs/adr/` 결과에서 `NNNN-*.md` 형태 파일을 수집 (`0000-template.md`, `README.md` 제외).
   - 가장 큰 NNNN + 1, 4자리 zero-pad.
3. `docs/adr/NNNN-<title>.md` 생성 — `docs/adr/0000-template.md` 구조를 그대로 사용하되 다음을 치환:
   - 첫 줄 `# NNNN. <제목>` — kebab-case 제목을 공백 구분 + 첫 글자 대문자로 변환.
   - 날짜 — 오늘 날짜 (YYYY-MM-DD).
   - 상태 — `Proposed`.
4. 배경 / 결정 / 대안 / 결과 섹션 내용은 사용자에게 묻는다. 인자 뒤에 요약 설명이 따라오면 초안으로 활용.
5. 생성된 파일 경로를 보고하고, 결정이 확정되면 상태를 `Accepted` 로 바꾸라고 안내.

## 규칙

- 번호 재사용 금지. 건너뛰기 금지 (연속).
- 기존 ADR 파일은 내용 수정하지 않는다. 결정이 바뀌면 새 ADR 생성 + 이전 ADR 상태를 `Superseded by NNNN` 으로만 갱신.
- 작성 대상·비대상 기준은 `docs/adr/README.md` 참조.
