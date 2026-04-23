---
name: chat-feature
description: 새로운 채팅 기능의 WebSocket handler + service + DTO + 테스트 스켈레톤을 생성한다. /chat-feature <feature-name>로 호출 (예 /chat-feature direct-message).
---

# chat-feature

ripple-chat에 새로운 WebSocket 기반 기능을 스캐폴딩한다.

## 선행 확인

1. 인자로 feature 이름을 받는다 (kebab-case 예: `direct-message`). 없으면 묻는다.
2. `src/main/kotlin` 하위 base package를 확인한다. 아직 패키지 구조가 없으면 사용자에게 base package 이름을 먼저 물어보고 CLAUDE.md를 갱신한다.
3. 기존 `WebSocketHandler` / `SimpleUrlHandlerMapping` 설정이 있으면 그 컨벤션을 따른다. 없으면 아래 템플릿으로 새로 만든다.

## 생성 파일 (<Name> = PascalCase 변환, <name> = camelCase)

- `handler/<Name>WebSocketHandler.kt`
  - `WebSocketHandler` 구현, 코루틴 기반. `session.receive().asFlow()` 수신 + `session.send(...)` 송신.
  - `try/finally`로 세션 정리.
- `service/<Name>Service.kt`
  - `@Service` 클래스, suspend 함수로 비즈니스 로직 노출.
- `dto/<Name>Message.kt`
  - 인바운드/아웃바운드 메시지 data class. 기존 직렬화 컨벤션(Jackson / kotlinx.serialization)을 따른다. 불명확하면 사용자에게 묻는다.
- `config/<Name>WebSocketConfig.kt` (또는 기존 설정에 추가)
  - `/ws/<name>` 경로에 핸들러 등록.
- `src/test/kotlin/.../handler/<Name>WebSocketHandlerTest.kt`
  - `@SpringBootTest(webEnvironment = RANDOM_PORT)` + `ReactorNettyWebSocketClient` + `runTest` 기반 skeleton.

## 규칙

- CLAUDE.md의 reactive/WebSocket 규칙을 따른다. 블로킹 호출, 원시 세션 저장, `Thread.sleep` 금지.
- 새로운 외부 의존성(R2DBC, Redis 등)은 **사용자 확인 없이 추가하지 않는다**. 필요하면 `build.gradle.kts` 변경 제안만 하고 중단한다.
- 인증/인가는 TODO 주석으로 표시만 하고 구현하지 않는다 (모델 미정).

## 완료 보고

- 생성된 파일 목록과 각 파일의 책임 1줄 요약.
- 다음 단계 제안 (예: 영속 계층 연결, handshake 인증 붙이기).
