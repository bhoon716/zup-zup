---
name: refactoring
description: 사용자가 외부에서 관찰 가능한 동작을 보존하면서 refactor, cleanup, simplify, extract, rename, duplication 제거, readability 개선, file 분리, separation of concerns 개선, testability 개선을 요청할 때 사용한다. 이 스킬은 Baseline → Transform → Same Tests 루프로 보수적이고 테스트 보호된 리팩토링을 수행하게 한다. 먼저 보존해야 할 동작 경계를 정의하고, 편집 전 baseline test 를 실행하고, 한 번에 하나의 작은 안전한 변환만 적용하고, 같은 테스트를 다시 실행하며, 동작 보존을 검증할 수 없으면 중단한다. 기능 개발, 버그 수정, 의도적 동작 변경, 추측성 재작성, 아키텍처 재설계, 의존성 업그레이드, 광범위한 cleanup 에는 사용하지 않는다.
---

# refactoring

## 목적

외부에서 관찰 가능한 동작을 보존하면서 코드 구조를 개선한다.

리팩토링은 테스트가 동작 보존을 증명하기 전까지 안전하지 않다고 취급한다. 중심 루프는 Baseline → Transform → Same Tests 다.

1. 바뀌면 안 되는 동작 경계를 정의한다.
2. 대상 코드나 동작을 보호하는 기존 테스트를 찾는다.
3. 편집 전에 그 테스트를 실행한다.
4. baseline 결과를 기록한다.
5. 작고 안전한 변환 하나를 적용한다.
6. 변환 뒤 같은 테스트를 다시 실행한다.
7. 같은 테스트가 계속 통과할 때만 계속한다.
8. 동작 보존을 검증할 수 없으면 중단, revert, 또는 scope 축소한다.

## 이 스킬을 사용할 때

요청의 주목적이 동작 보존 상태에서 내부 코드 구조를 바꾸는 것일 때 사용한다.

예시:

- 모듈 리팩토링
- 컴포넌트 cleanup
- 함수 단순화
- helper 추출
- 중복 제거
- 가독성 개선
- 큰 파일 분리
- 명확성을 위한 method rename
- 관심사 분리 개선
- 동작 변경 없이 테스트하기 쉽게 만들기

## 이 스킬을 사용하지 않을 때

다음이 주목적이면 사용하지 않는다.

- 새 동작 추가. `feature-dev`를 사용한다.
- 깨진 동작 수정. `bugfix`를 사용한다.
- 조사만 하는 작업.
- 의존성 업그레이드만 하는 작업.
- 의도적 아키텍처 변경.
- 의도적 제품 동작 변경.
- 처음부터 재작성.

버그 수정이나 기능 개발과 섞이면 조용히 섞지 않는다. 깨진 동작은 `bugfix`로 먼저 고치고, 새 동작은 `feature-dev`로 먼저 구현한다. 이 스킬은 동작 보존 cleanup 부분에만 사용한다.

## 핵심 규칙: 테스트 보호 리팩토링만 허용

하지 말아야 할 일:

- 새 제품 동작 추가.
- 무관한 버그 수정.
- 명시 요청 없이 API contract 변경.
- 명시 요청 없이 UI behavior 변경.
- 명시 요청 없이 DB schema 변경.
- permission, auth, billing, privacy, security, data-loss behavior 변경.
- 리팩토링이라는 이름으로 아키텍처 재작성.
- 기능 개발과 리팩토링 결합.
- 광범위한 의존성 업그레이드와 리팩토링 결합.
- 표준 workflow 없이 generated, vendor, lock, build output 수정.
- 실패하는 테스트 삭제.
- 동작 coverage 를 유지하지 않은 채 내부 구현에 맞게 테스트만 수정.
- 테스트나 동등 검증 없이 동작 보존 주장.

## 워크플로: Baseline → Transform → Same Tests

### 1. 리팩토링 목적

편집 전 리팩토링 이유를 정확히 식별한다.

가능한 목적:

- 중복 감소
- 가독성 개선
- 함수, 클래스, 컴포넌트, 모듈, 서비스 추출
- 이름 개선
- control flow 단순화
- side effect 격리
- coupling 감소
- cohesion 개선
- 책임 분리
- testability 개선
- complexity 감소
- 사용되지 않음이 입증된 dead code 제거
- 기존 패턴 정규화

편집 전 목적을 한두 문장으로 밝힌다. “더 좋게 만들기” 같은 모호한 목적은 broad change 의 근거가 될 수 없다.

### 2. 동작 경계

무엇이 바뀌면 안 되는지 정의한다.

보존해야 할 것:

- public API shape
- function signature
- return value
- internal-only 가 아닌 error type/message
- UI text 와 visible state
- route, command, flag, config 이름
- DB read/write, migration, transaction semantics
- authentication/authorization behavior
- billing, privacy, security, data-loss behavior
- 외부에서 소비되거나 테스트되는 logging/metrics
- relevant performance characteristics
- timing, concurrency, caching, retry behavior

동작 경계를 식별할 수 없으면 리팩토링 범위를 줄이거나 중단한다.

### 3. 컨텍스트 탐색

편집 전 local convention 을 확인한다.

- `AGENTS.md`, `CLAUDE.md`, `.cursor/rules`, `README.md`, `CONTRIBUTING.md`
- `package.json`, `pyproject.toml`, `Cargo.toml`, `go.mod`, `pom.xml`, `build.gradle`, `Makefile`, `justfile`, `Taskfile.yml`
- `jest.config.*`, `vitest.config.*`, `pytest.ini`, `tox.ini`, `playwright.config.*`, `cypress.config.*`, `rspec`, `phpunit.xml`, `.github/workflows`
- 기존 테스트 디렉터리와 naming convention
- 대상 코드를 보호하는 기존 테스트
- 유사 코드 경로와 유사 테스트
- public interface, API contract, UI state, persistence, side effect, logs, metrics, permission, error behavior
- generated file convention 과 formatting/linting command

새 패턴을 만들기보다 기존 관례를 우선한다.

### 4. 테스트 탐색

편집 전에 기존 테스트를 찾는다.

- unit tests
- integration tests
- component tests
- API/contract tests
- snapshot tests
- e2e tests
- golden file tests
- type-level tests
- build/typecheck/lint checks
- CI workflow commands

관련 테스트가 없으면 가능할 때 characterization test 를 추가한다. 현재 동작을 포착하는 가장 작은 테스트를 만들고, 새 기대 동작을 발명하지 않는다. 어렵다면 mechanical low-risk change 로 제한하고 약한 보호 상태라고 보고한다.

### 5. Baseline test run

프로덕션 코드 편집 전 관련 테스트를 실행한다.

기록한다.

- 실행 명령
- 결과
- 실패 요약
- 실패가 target code 와 관련 있는지
- 계속 진행해도 안전한 baseline 인지

target area baseline 이 red 이면 broad refactor 를 진행하지 않는다. 먼저 `bugfix`를 사용하거나 방향을 묻는다.

### 6. 리팩토링 계획

리팩토링을 작은 변환으로 나눈다. mechanical change 와 logic-looking transformation 은 가능한 분리한다. 많은 파일을 건드리는 rename 은 logic change 와 결합하지 않는다.

### 7. Transform phase

한 번에 의미 있는 변환 하나만 적용한다.

규칙:

- diff 를 작고 reviewable 하게 유지한다.
- 무관한 변환을 결합하지 않는다.
- 동작 coverage 를 유지하지 못하면 테스트를 내부 구현에 맞게 바꾸지 않는다.
- 새 동작을 도입하지 않는다.
- 리팩토링 중 발견한 무관 이슈를 고치지 않는다.
- 명시 목적을 벗어난 style preference 를 쫓지 않는다.
- 각 변환 뒤 diff 를 확인해 accidental behavior change 를 찾는다.

### 8. Same Tests phase

각 의미 있는 변환 뒤 baseline 에 사용한 같은 테스트를 다시 실행한다.

- 가능하면 동일 명령을 실행한다.
- 결과를 baseline 과 비교한다.
- 통과하면 계속한다.
- 실패하면 변환 때문인지 식별한다.
- 변환 때문이면 수정하거나 revert 한다.
- 무관하거나 flaky 하면 증거를 기록하고 scope 를 넓히지 않는다.
- 실패 상태 위에 추가 변환을 쌓지 않는다.

### 9. 더 넓은 검증

계획한 변환이 끝나면 먼저 같은 baseline test 를 실행하고, 가능하면 검증을 넓힌다.

- 관련 테스트 suite
- typecheck
- lint
- build
- formatting check
- API contract tests
- UI/component/e2e tests
- performance-sensitive code 의 performance check

### 10. 최종 리뷰

보고 전 최종 diff 를 확인한다.

- public behavior 가 바뀌었는가?
- test expectation 이 바뀌었는가?
- public interface 가 바뀌었는가?
- error message/status code 가 바뀌었는가?
- permission/security behavior 가 바뀌었는가?
- persistence behavior 가 바뀌었는가?
- logging/metrics behavior 가 바뀌었는가?
- 무관 cleanup 이 들어갔는가?
- diff 가 rewrite 보다 작고 명확한가?
- 검증 주장은 실제 실행 명령으로 뒷받침되는가?

### 11. 최종 보고

테스트나 동등 검증 없이 동작 보존을 주장하지 않는다.

포함한다.

- 리팩토링 목적
- 동작 보존 경계
- 변경 파일
- 편집 전 baseline tests
- 변환 뒤 same tests
- 추가 검증 명령과 결과
- 추가한 characterization tests
- 실행하지 못한 테스트
- 알려진 위험 또는 follow-up
- 테스트가 없어 약하게 보호된 refactor 인 경우 명시

## 안전한 변환

- local variable rename
- function/component/constant/helper 추출
- 불필요한 indirection inline
- 명백한 중복 제거
- truth table 을 바꾸지 않는 conditional 단순화
- logic 변경 없는 code move
- pure logic 과 side effect 분리
- 기존 project abstraction 사용
- public contract 변경 없는 module boundary 개선
- 사용되지 않음이 강하게 입증된 dead code 삭제

## 위험한 변환

명시 요청 없이 피하거나 추가 주의가 필요하다.

- public API 변경
- DB schema/migration 변경
- authorization logic 변경
- error semantics 변경
- concurrency/async ordering 변경
- caching/retry behavior 변경
- time/timezone/locale/encoding/rounding 변경
- serialization/deserialization 변경
- dependency version 변경
- subsystem 전체 교체
- 동작 코드 처음부터 재작성
- large file move 와 logic change 결합

## Acceptance criteria checklist

- 목적이 구체적이다.
- 동작 경계가 명시됐다.
- 편집 전 baseline verification 을 실행했거나 한계를 명시했다.
- 각 변환이 작고 reviewable 하다.
- 가능한 경우 같은 테스트를 변환 후 다시 실행했다.
- 새 동작을 넣지 않았다.
- 무관 bugfix 를 섞지 않았다.
- 명시 요청 없이 public contract 를 바꾸지 않았다.
- 최종 검증 증거가 있다.

## Test protection checklist

편집 전:

- 관련 기존 테스트 식별.
- coverage 가 없고 가능하면 characterization test 추가.
- baseline tests 실행 및 기록.
- target area baseline 이 red 이면 중단하거나 scope 축소.

변환 후:

- 같은 테스트 재실행.
- baseline 과 비교.
- refactor 때문에 실패하면 중단.
- 불확실성을 숨기지 않기.

## Verification command strategy

1. 대상 동작을 보호하는 가장 작은 targeted test
2. 각 변환 뒤 같은 targeted test
3. 관련 테스트 suite
4. typecheck
5. lint
6. build
7. formatting check
8. 관련 contract/UI/e2e/performance checks

실행 가능한 동작 테스트가 있고 변경이 순수 mechanical 이 아닌 한 typecheck/lint 만으로 동작 보존을 증명하지 않는다.

## 결정 규칙

- 관련 테스트가 없으면 가능할 때 characterization tests 를 먼저 추가한다.
- characterization tests 가 불가능하면 mechanical low-risk change 로 제한하고 약한 보호를 보고한다.
- target area baseline 이 red 이면 먼저 `bugfix`를 사용한다.
- target 밖 baseline 이 red 이면 narrow scope 로만 진행하고 pre-existing failure 를 보고한다.
- public behavior 변경이 필요하면 `feature-dev` 또는 `bugfix`로 재분류한다.
- bug 를 발견하면 조용히 고치지 말고 보고하거나 `bugfix`로 전환한다.
- broad architectural redesign 이 필요하면 명시 승인을 요청하거나 별도 design task 로 분리한다.
- generated 파일은 표준 생성 명령으로만 갱신한다.
- 동작 보존을 검증할 수 없으면 refactor 확장을 중단한다.

## 실패 처리

post-transform test 실패는 중단 신호다.

- 실패 중에는 추가 변경을 쌓지 않는다.
- refactor 가 원인인지 먼저 판단한다.
- 원인이 refactor 이면 수정하거나 revert 한다.
- 무관하면 증거를 기록하고 scope 를 넓히지 않는다.
- flaky 면 합리적인 경우 한 번 재실행하고 불확실성을 보고한다.
- baseline 이 clean 하지 않았다면 완전히 검증됐다고 주장하지 않는다.

## Anti-patterns

- “하는 김에” cleanup
- 리팩토링과 기능 개발을 한 diff 에 섞기
- 리팩토링과 버그 수정을 한 diff 에 섞기
- 안전한 변환 대신 모듈 재작성
- behavior coverage 를 잃으며 테스트를 내부 구현에 맞추기
- 실패 테스트 삭제
- public API shape 우발 변경
- error message/status code 우발 변경
- auth, billing, privacy, data-loss behavior 우발 변경
- file move 와 logic change 결합
- formatting-only 대형 diff 와 logic-looking change 결합
- baseline tests 재실행 없이 equivalence 주장
- typecheck/lint 만으로 behavior preservation 주장

## Final response template

Summary:
- <refactoring objective and result>

Behavior preservation:
- Boundary preserved: <public behavior, API, UI, persistence, errors, permissions, etc.>
- Characterization added: <yes/no, details>

Files changed:
- `<path>`: <reason>

Verification:
- Baseline before editing: `<command>` → <result>
- Same tests after transform: `<command>` → <result>
- Additional checks: `<command>` → <result>

Notes:
- <pre-existing failures, skipped checks, weak test coverage, risks, or follow-ups>
