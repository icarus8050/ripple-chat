# Architecture Decision Records

비자명한 기술/설계 의사결정을 짧은 문서로 남긴다. **하나의 파일 = 하나의 결정**.

## 파일 이름

`NNNN-kebab-case-title.md`. NNNN은 4자리 순번 (0001부터, **재사용 금지**).

예: `0001-use-r2dbc-for-persistence.md`, `0007-broadcast-via-redis-pubsub.md`

## 내용 (템플릿은 [0000-template.md](0000-template.md))

각 ADR은 다음을 포함:

- **상태** — Proposed / Accepted / Deprecated / Superseded by NNNN
- **날짜** — YYYY-MM-DD
- **배경 (Context)** — 왜 이 결정이 필요했는지. 제약·요구사항·트리거.
- **결정 (Decision)** — 무엇을 선택했는지. 한 문단이면 충분.
- **대안 (Alternatives considered)** — 고려했지만 채택하지 않은 선택지와 각하 이유.
- **결과 (Consequences)** — 이 결정이 이후 작업에 미치는 영향 (긍정/부정 양쪽).

## 언제 기록하는가

**필요**
- 기술 스택 선택 (영속 계층, 메시지 브로커, 인증/인가 방식)
- 일관성·가용성·성능의 트레이드오프가 있는 설계 (at-most-once vs at-least-once, 캐시 전략 등)
- 명시적으로 각하된 강한 대안이 있는 선택
- 외부 제약으로 한 결정 (보안·컴플라이언스·레거시 호환)

**불필요**
- 코드 스타일·포맷팅
- 단순 버그 수정이나 작은 리팩토링
- CLAUDE.md 규칙을 그대로 적용한 결정
- 쉽게 되돌릴 수 있는 변경

## 수정 규칙

- **Accepted 된 ADR은 편집하지 않는다.** 결정을 바꾸려면 새 ADR을 만들고, 이전 ADR의 상태만 `Superseded by NNNN` 으로 갱신.
- 오타·링크 같은 비의미적 수정은 허용.

## 생성 방법

`/decision-log <kebab-case-title>` 슬래시 명령으로 번호를 자동 할당해 생성. 예: `/decision-log use-r2dbc-for-persistence`.
