---
name: reactive-review
description: 변경된 Kotlin 파일에서 WebFlux/Coroutine 안티패턴을 검토한다. 블로킹 호출, Dispatcher 누락, WebSocket 세션 누수, Mono/Flux 오용을 점검. /reactive-review로 호출.
---

# reactive-review

ripple-chat에서 변경된 Kotlin 소스를 reactive/coroutine 관점에서 리뷰한다.

## 절차

1. 변경된 파일 식별
   - `git status --porcelain` 및 `git diff --name-only` 로 staged/unstaged 파일을 모두 수집.
   - 확장자가 `.kt`인 파일만 대상으로 한다.

2. 각 파일에 대해 다음 항목을 점검 (발견 시 `file:line` 과 함께 기록)
   - **블로킹 호출**: `.block()`, `.blockFirst()`, `.blockLast()`, `.toFuture().get()`, `Thread.sleep`, JDBC (`DriverManager`, `PreparedStatement.execute*`), `java.io.File.readText/writeText` (대용량), `URL.openStream`.
   - **잘못된 `runBlocking`**: `main()`·테스트·명시적 adapter 외 위치에서의 `runBlocking`.
   - **Dispatcher 누락**: blocking 호출이 확인됐는데 `withContext(Dispatchers.IO)` 없이 실행.
   - **Reactive subscribe 오용**: controller/handler/service 내부에서 `.subscribe { }` 호출 후 값을 버리는 패턴 (프레임워크에 반환하지 않음).
   - **WebSocket 세션 누수**: `WebSocketSession`을 companion object / singleton / 일반 `MutableMap`에 저장하거나, `try/finally`로 cleanup 하지 않는 핸들러.
   - **브로드캐스트 안티패턴**: 세션 리스트를 순회하며 직접 `send` 호출 (대신 `MutableSharedFlow` / `Sinks.Many.multicast()` 권장).
   - **Flow/Flux 혼용 실수**: `Flux` 체인 내부에 `suspend` 람다를 `map`에 바로 넘기는 경우 — `.asFlow()` 경유 필요.
   - **Handshake 후 재검증 중복**: handler 내부에서 이미 handshake에서 끝난 인증/권한 체크를 반복.

3. 보고
   - 발견 항목을 severity(critical / warning / nit)로 분류해 punch list로 제시.
   - 각 항목에 "왜 문제인가" 한 줄 + 제안 수정 방향 한 줄.
   - 발견 없으면 한 문장으로 그렇게 보고.

수정은 하지 않는다. 리뷰 결과만 보고한다.
