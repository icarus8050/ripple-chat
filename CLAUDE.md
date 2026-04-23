# ripple-chat

실시간 채팅 서비스. **Spring Boot 3 + Kotlin + JDK 21 + WebFlux + Coroutines + WebSocket**.

영속 계층(R2DBC / MongoDB Reactive / Redis 등) 및 base package는 아직 미정 — 확정 시 이 문서를 갱신한다.

## Reactive / Coroutine 규칙 (strict)

- Reactive chain 또는 suspend 함수 내부에서 블로킹 API 호출 금지. 해당: JDBC, `Thread.sleep`, `.block()`, `.toFuture().get()`, 블로킹 HTTP 클라이언트, 동기 파일 I/O.
- 서비스 레이어는 suspend function을 기본으로 하고, boundary에서만 `kotlinx-coroutines-reactor`(`awaitSingle`, `awaitFirstOrNull`, `asFlow`, `asPublisher`)로 변환한다.
- 블로킹이 불가피하면 `withContext(Dispatchers.IO)`로 감싼다.
- `runBlocking`은 `main()`·테스트·명시적 adapter 레이어 밖에서 사용하지 않는다.
- Controller/Handler 반환 타입은 `suspend` 함수, `Flow<T>`, `Mono<T>`, `Flux<T>` 중 하나.

## WebSocket

- `WebSocketHandler` 구현체는 코루틴 기반으로 처리한다. 수신은 `session.receive().asFlow()`, 송신은 `session.send(...)`.
- 등록은 `SimpleUrlHandlerMapping` + `WebSocketHandlerAdapter` 조합.
- 세션 종료 시 반드시 정리: `try/finally`로 구독 해제, 공유 상태에서 세션 ID 제거.
- 브로드캐스트는 `MutableSharedFlow` 또는 `Sinks.Many.multicast()`로. 원시 `WebSocketSession`을 앱 상태에 저장하지 않는다.
- 인증/인가는 handshake 단계에서 검증하고 세션 attributes에 주입한다. handler 진입 후 재검증 로직을 복제하지 않는다.

## 테스트

**엔진**: JUnit 5 (Jupiter). `build.gradle.kts`에 `tasks.test { useJUnitPlatform() }` 유지.

- Import는 반드시 `org.junit.jupiter.api.*`. JUnit 4 (`org.junit.Test`, `@RunWith`) 사용 금지.
- 라이프사이클: `@BeforeEach` / `@AfterEach` / `@BeforeAll` / `@AfterAll`. `@BeforeAll`·`@AfterAll`은 `companion object` + `@JvmStatic` 로 선언하거나, 클래스에 `@TestInstance(Lifecycle.PER_CLASS)`를 붙여 일반 메서드로 사용.
- 구조: 관련 시나리오는 `@Nested` inner class로 묶고, 테스트 이름은 `@DisplayName("한글 시나리오 설명")` 권장.
- 파라미터화: `@ParameterizedTest` + `@ValueSource` / `@CsvSource` / `@MethodSource`. `@MethodSource`용 함수는 `companion object`에 `@JvmStatic`으로 선언.
- 예외/그룹 단언: `assertThrows`, `assertAll` (`org.junit.jupiter.api.Assertions`). 가독성 개선이 필요하면 AssertJ / Kotest matcher 도입 제안 — 의존성 추가는 사용자 확인 후.
- 의존성 (추후 추가 시 권장):
  - `testImplementation("org.junit.jupiter:junit-jupiter")`
  - `testRuntimeOnly("org.junit.platform:junit-platform-launcher")`
  - Kotlin 어댑터: `testImplementation(kotlin("test-junit5"))` — 현재 `build.gradle.kts`에 있는 `kotlin("test")`를 이것으로 교체하면 JUnit 5 전용으로 고정.
  - Spring Boot 추가 시 `spring-boot-starter-test`가 JUnit Jupiter를 이미 포함하므로 중복 선언 불필요.

**스타일**

- Unit: suspend 함수는 `runTest { ... }` (kotlinx-coroutines-test), Reactor pipeline은 `StepVerifier`.
- Integration: `@SpringBootTest(webEnvironment = RANDOM_PORT)` + `WebTestClient`. WebSocket은 `ReactorNettyWebSocketClient`.
- 시간 대기는 `runTest`의 virtual time 또는 `StepVerifier.withVirtualTime` 사용. `Thread.sleep` 금지.
- 테스트 클래스/메서드에 Kotlin `open` 불필요 (JUnit 5는 final 메서드도 호출 가능).

## 빌드 / 실행

- Build: `./gradlew build`
- Test: `./gradlew test`
- Run: `./gradlew bootRun`

## 패키지 구조 (제안)

```
src/main/kotlin/<base>/
  config/     # @Configuration, WebSocket/Router 매핑
  handler/    # WebSocketHandler, HandlerFunction
  service/    # suspend / reactive 비즈니스 로직
  domain/     # 엔티티, 값 객체
  dto/        # 요청/응답 DTO
```

## 주석 규칙

- **기본은 "주석 없음"**. 잘 지어진 식별자와 타입 시그니처가 WHAT을 말해준다. 주석을 지워도 읽는 사람이 혼동하지 않으면 쓰지 않는다.
- **허용 (WHY가 비자명할 때만)**: 숨은 제약, 미묘한 불변식, 특정 버그를 우회하는 임시 코드, 읽는 이를 놀라게 할 동작. 주석에는 "왜"와 "언제 제거 가능한지"를 함께 적는다.
- **금지**
  - WHAT 반복 설명 — 식별자가 이미 말하는 내용을 주석으로 되풀이하지 않는다.
  - 현재 태스크·호출자·이슈 번호 (`// X 플로우에서 사용`, `// JIRA-123 대응`) — PR 설명·커밋 메시지·ADR의 역할. 시간이 지나면 부정확해진다.
  - 변경 이력 (`// 2026-04-24 수정`, `// was: ...`) — `git log` / `git blame`이 권위 있는 출처.
  - 삭제된 코드의 흔적 (`// removed X`) — 그냥 지운다.
  - 오너·맥락 없는 TODO (`// TODO: 나중에 처리`). 달려면 `// TODO(<owner> or ADR-NNNN): 무엇을 왜` 형식.
- **KDoc** (`/** ... */`): public API 중 **호출 측이 코드만 보고 알기 어려운 계약**만 기록. 예: suspend 함수의 취소 동작, 던지는 예외 조건, 스레딩 가정, 누적/멱등성 보장. 파라미터 이름을 반복 설명하는 KDoc은 쓰지 않는다.

**예시**

```kotlin
// OK — WHY (우회 이유 + 제거 조건)
// Reactor Netty 1.1.8의 Context 누락 회피 — 1.1.9 이상에서 제거 가능
val ctx = Hooks.captureContext()

// NG — WHAT 반복
// 사용자 조회
fun findUser(id: UserId): User? = ...

// NG — 오너/맥락 없는 TODO
// TODO: 나중에 캐시 추가
```

## 작업 피드백 루프

작업 단위(feature / fix / refactor) 하나당 다음 사이클을 돌린다. **작게 자주**.

1. **구현** — 동작하는 코드 먼저. 이 단계는 정확성 우선.
2. **자가 리뷰** — `/self-review` 로 CLAUDE.md 규칙 전반(Reactive / WebSocket / 주석 / 테스트 / 패키지 구조 / 빌드 hygiene)을 점검. 결과는 `Critical` / `Warning` / `Nit` 로 분류된 punch list.
3. **선별**
   - **Critical**: 반드시 수정.
   - **Warning**: 원칙적으로 수정. 유예 사유가 명확하면 `TODO(ADR-NNNN)` 로 명시하고 ADR 작성.
   - **Nit**: 같은 커밋 범위에서만 해결. 분리 필요하면 건너뛴다.
4. **리팩토링** — 동작 유지, 구조만 변경. 기능 추가와 **섞지 않고 별도 커밋**.
5. **검증** — `./gradlew test` 통과. Reactive 수정이면 `/self-review` 2차 확인 (회귀 방지).
6. **커밋** — 커밋 가이드라인대로. 기능 / 리팩토링 / 테스트 / 문서는 **각각 커밋 분리**.
7. **회고** — 이 루프에서 반복해 잡은 이슈가 있거나, 리뷰가 놓친 실수가 있었다면 `/skill-tune` 으로 스킬 혹은 이 문서를 보강.

### 스킬 관리 원칙

`.claude/skills/` 는 팀(현재 1인) 지식의 실행 가능 버전. 시간에 따라 보강한다.

- **추가**: 두 번 이상 반복한 흐름, 또는 두 번 이상 놓친 이슈 유형일 때만. 일회성에는 만들지 않는다.
- **업데이트**: 기존 스킬이 놓친 케이스가 발견되면 해당 스킬 체크리스트에 한 줄 추가. 프롬프트는 짧게, 규칙은 bullet로.
- **폐기**: 스택 변경·패턴 폐기로 더 이상 적용되지 않는 스킬은 삭제. 남기면 잘못된 컨텍스트가 주입된다.
- **변경은 별도 커밋** — 스킬만 바뀔 때 `chore(skills): ...`, 규칙 문서만 바뀔 때 `docs: ...`. 둘을 섞지 않는다.

## 의사결정 로그 (ADR)

비자명한 기술/설계 결정은 `docs/adr/` 에 Architecture Decision Record로 남긴다.

- **기록 대상**: 기술 스택 선택 (영속 계층, 브로커, 인증), 일관성·성능 트레이드오프가 있는 설계, 명시적으로 각하된 강한 대안이 있는 선택, 외부 제약으로 한 결정.
- **기록 불필요**: 스타일, 작은 리팩토링, 본 문서 규칙의 단순 적용, 쉽게 되돌릴 수 있는 변경.
- **작성 방법**: `/decision-log <kebab-case-title>` — 다음 순번 할당·템플릿 채움·사용자 질의까지 자동.
- **수정 규칙**: Accepted 된 ADR은 수정하지 않는다. 변경이 필요하면 새 ADR을 만들고 이전 ADR 상태를 `Superseded by NNNN` 으로 갱신.

상세 형식·분류 기준은 `docs/adr/README.md`.

## 커밋 가이드라인

**메시지 형식** — Conventional Commits

```
<type>(<scope>): <subject>

<body>

<footer>
```

- **type**: `feat` | `fix` | `refactor` | `test` | `docs` | `chore` | `perf` | `build` | `ci`
- **scope** (선택): 영향 영역 — `handler`, `service`, `config`, `dto`, `ws`, `build`, `adr` 등
- **subject**: 70자 이내, 동사로 시작. 한국어/영어는 한 프로젝트 안에서 일관되게. 마침표 없음.
  - OK: `feat(handler): DM WebSocket 핸들러 추가`
  - OK: `fix(service): 세션 종료 시 sink 구독 누수 수정`
  - NG: `DM 기능`, `버그 수정`, `WIP`

**본문 (body)**

- subject만으로 불충분할 때만 작성. **why**에 집중 — **what**은 diff로 확인 가능.
- 72자 랩, subject와 빈 줄로 구분.
- 관련 ADR이 있으면 footer에 `Refs: docs/adr/NNNN-<slug>.md` 로 참조.

**커밋 단위**

- 하나의 논리적 변경 = 하나의 커밋. 리팩토링과 기능 추가를 한 커밋에 섞지 않는다.
- 각 커밋은 독립적으로 빌드·테스트가 통과하는 상태여야 한다.
- 메시지로 설명하기 어려우면 단위를 쪼개는 신호다.

**금지**

- 이미 원격에 푸시된 커밋에 `--amend` / `rebase` 금지 (본인 feature 브랜치 제외).
- `--no-verify`로 훅 우회 금지. 훅 실패 시 원인을 고치고 새 커밋을 만든다 (amend 금지 — 훅 실패는 커밋이 안 된 상태).
- `main`에 WIP 커밋 금지. feature 브랜치에서만 허용하고 merge 전 squash.
- `git add -A` / `git add .` 기본 금지. `.env`, 시크릿, 대용량 바이너리 실수 방지를 위해 파일을 명시적으로 지정.

**커밋 전 체크리스트**

- `./gradlew test` 통과.
- 변경 범위에 Kotlin 파일이 있으면 `/reactive-review` 실행.
- 비자명한 결정이 포함됐다면 `/decision-log <title>` 로 ADR을 먼저 남기고, 커밋 footer에서 참조.

**예시**

```
feat(handler): DM WebSocket 핸들러 추가

1:1 메시지 송수신 플로우 구현. 세션 종료 시 공유 sink에서
구독을 제거해 누수를 방지.

Refs: docs/adr/0003-direct-message-model.md
```

## 커스텀 skill

- `/self-review` — 변경된 파일 전체를 CLAUDE.md 규칙 기준으로 자가 리뷰하고 `Critical`/`Warning`/`Nit` punch list 출력 (수정 없음). 작업 피드백 루프의 기본 진입점.
- `/reactive-review` — 변경된 Kotlin 파일에서 블로킹 호출·세션 누수 등 reactive 안티패턴만 집중 점검 (`/self-review`의 부분 집합; 독립 호출이 필요할 때만).
- `/chat-feature <name>` — WebSocket handler + service + DTO + 테스트 스켈레톤 생성.
- `/decision-log <title>` — `docs/adr/` 에 새 ADR 파일을 다음 순번으로 생성.
- `/skill-tune` — 반복 이슈를 기존 스킬 체크리스트·CLAUDE.md 규칙·새 스킬로 환원 (메타 스킬).
