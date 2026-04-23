---
name: self-review
description: 변경된 파일 전체를 ripple-chat의 CLAUDE.md 규칙(Reactive / WebSocket / 주석 / 테스트 / 패키지 구조) 기준으로 자가 리뷰하고 punch list를 낸다. 코드는 수정하지 않는다. /self-review로 호출.
---

# self-review

작업 피드백 루프의 **리뷰 단계**. 변경 파일 전체에 대한 종합 자가 리뷰를 수행하고 우선순위화된 punch list를 출력한다. **수정은 하지 않는다**.

## 절차

1. **대상 파일 수집**
   - `git status --porcelain` + `git diff --name-only HEAD`
   - 관심 확장자: `.kt`, `.kts`, `.md` (`docs/adr/**`), `.json` (`.claude/**`).

2. **Kotlin 소스 (`.kt`) 점검**
   - **Reactive / Coroutine**: 블로킹 호출 (`.block()`, `Thread.sleep`, JDBC, `runBlocking` 부적절 사용), 블로킹 시 `Dispatchers.IO` 누락, Mono/Flux subscribe 오용, `runBlocking` 위치.
   - **WebSocket**: `WebSocketSession` 원시 참조가 공유 상태에 저장되는가? `try/finally` cleanup 누락? 브로드캐스트에 sink/SharedFlow가 아닌 raw 세션 순회?
   - **주석**: WHAT 반복 주석, 태스크/이슈/호출자 참조, 변경 이력 주석, 오너·맥락 없는 TODO, 삭제 흔적 주석, 파라미터 이름만 반복하는 KDoc.
   - **테스트**: production 변경에 대응 테스트가 있는가? JUnit 4 import (`org.junit.Test`, `@RunWith`) 잔존? `Thread.sleep` 사용? `runTest` / `StepVerifier` 사용 여부? `@DisplayName` / `@Nested` 활용?
   - **패키지 구조**: `handler/` · `service/` · `config/` · `dto/` · `domain/` 경계 위반? 비즈니스 로직이 handler에 새는지? DTO가 domain을 역참조하는지?
   - **네이밍**: 의미 없는 단축어, 타입 이름 반복 (`userUser`), 한/영 혼용 불일치.

3. **빌드 / 의존성 점검** (`build.gradle.kts` 변경 시)
   - 새 의존성이 추가됐는가? 추가가 ADR 없이 이루어졌다면 `docs/adr/` 에 기록이 필요한 결정인지 판단.
   - Spring Boot starter 추가 시 중복 선언 (예: `spring-boot-starter-test` 있는데 `junit-jupiter` 별도 선언).

4. **설정 / 스킬 점검** (`.claude/**`, `docs/adr/**`)
   - ADR 번호 건너뛰기, Accepted ADR의 의미적 수정, 스킬 프롬프트가 과도하게 길어진 경우 플래그.

5. **출력 형식**

   ```
   ### Critical (반드시 수정)
   - path/to/File.kt:42 — 문제 한 줄 — 제안 한 줄

   ### Warning (수정 권장)
   - ...

   ### Nit (선택)
   - ...

   ### 루프 회고
   - 반복 패턴 있음/없음. 있다면 /skill-tune 후보: <한 줄>
   ```

## 규칙

- **수정 금지**. 점검 결과만 보고한다.
- 항목이 없으면 해당 섹션을 생략하지 말고 "없음"이라고 명시.
- 이미 다른 스킬(`/reactive-review`)과 겹치는 항목은 **한 번만 보고**. `/self-review` 가 상위 집합이므로 실행 시 `/reactive-review` 별도 호출 불필요.
- 마지막 "루프 회고" 줄은 항상 포함 — 반복 패턴이 관찰되면 `/skill-tune` 후보로 한 줄 제안, 없으면 "없음"으로.
