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

- `/reactive-review` — 변경된 Kotlin 파일에서 블로킹 호출·세션 누수 등 reactive 안티패턴을 점검.
- `/chat-feature <name>` — WebSocket handler + service + DTO + 테스트 스켈레톤 생성.
- `/decision-log <title>` — `docs/adr/` 에 새 ADR 파일을 다음 순번으로 생성.
