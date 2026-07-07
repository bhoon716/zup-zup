---
name: bugfix
description: 사용자가 깨진 동작, 잘못된 동작, 실패하는 테스트, flaky 동작, 회귀된 동작을 고치라고 요청할 때 사용한다. 예를 들면 failing test, 잘못된 status code, crash, 무시되는 CLI flag, 중복 output, 잘못 렌더링되는 UI, 무한 retry, stack trace, 예전에는 됐지만 지금은 안 되는 동작이다. 이 스킬은 Reproduce → Root Cause → Regression Test → Minimal Fix → Verify 루프로 증거 기반 버그 수정을 수행하게 한다. 증상을 포착하고, 실패를 재현하거나 대표 regression case 로 표현하고, expected/actual behavior 를 정의하고, root cause 를 좁힌 뒤, 가능하면 먼저 실패하는 regression test 를 추가/갱신하고, 최소 안전 수정만 적용하고, 검증을 정직하게 보고한다. 새 기능 개발, 동작 보존 리팩토링, 추측성 cleanup, 아키텍처 재설계, 의존성 업그레이드, 문서만 수정, 테스트 정리만 하는 작업, 수정 없는 일반 조사에는 사용하지 않는다.
---

# bugfix

## 목적

깨진 동작, 잘못된 동작, 실패하거나 flaky 하거나 회귀된 동작을 증거 기반으로 수정한다.

원래 실패 또는 대표 regression test 가 고쳐졌다고 검증되기 전까지 bug fix 는 입증되지 않은 것으로 취급한다. 중심 루프는 Reproduce → Root Cause → Regression Test → Minimal Fix → Verify 다.

1. 보고된 증상을 포착한다.
2. 실패를 재현하거나 기존 failing test 를 찾는다.
3. expected behavior 와 actual behavior 를 확인한다.
4. root cause 를 국소화한다.
5. 버그 때문에 실패하는 regression test 를 추가하거나 갱신한다.
6. regression test 가 기대한 이유로 실패하는지 확인한다.
7. root cause 를 해결하는 최소 안전 수정을 적용한다.
8. regression test 를 다시 실행해 통과를 확인한다.
9. 관련 검증을 실행한다.
10. 재현, root cause, 수정, 검증을 정직하게 보고한다.

## 이 스킬을 사용할 때

요청의 주목적이 깨진 동작 수정일 때 사용한다.

예시:

- 실패하는 테스트 수정
- endpoint 가 잘못된 status code 반환
- login button 이 아무 동작을 하지 않음
- empty input 에서 app crash
- export 에 중복 row 포함
- job 이 무한 retry
- component 가 잘못된 값 렌더링
- CLI flag 가 무시됨
- regression 수정
- stack trace 조사 및 수정
- 예전에는 동작했지만 지금은 동작하지 않음

## 이 스킬을 사용하지 않을 때

다음이 주목적이면 사용하지 않는다.

- 새 동작 추가. `feature-dev`를 사용한다.
- 동작 보존 구조 개선. `refactoring`을 사용한다.
- 수정 요청 없는 조사만 하는 작업.
- 문서만 수정.
- 테스트 정리만 하는 작업.
- 버그 수정에 필요하지 않은 의존성 업그레이드.
- 의도적 제품 동작 변경.

버그 수정과 리팩토링 또는 기능 개발이 섞이면 먼저 가장 작은 변경으로 버그를 고친다. opportunistic cleanup 을 피하고, 의도 동작 복구에 필요한 경우가 아니면 새 동작을 추가하지 않는다. 버그가 검증된 뒤 구조 정리는 `refactoring`, 새 동작은 `feature-dev`로 분리한다.

## 핵심 규칙: 회귀 테스트 기반 버그 수정만 허용

하지 말아야 할 일:

- 무관한 새 동작 추가.
- 광범위한 무관 리팩토링.
- root cause 에 필요하고 사용자가 명시 승인하지 않은 아키텍처 재작성.
- 조사 중 발견한 무관 버그 수정.
- root cause 없이 증상만 가리기.
- build 를 통과시키기 위해 failing test 삭제/약화.
- 버그 수정 범위를 넘는 public behavior 변경.
- 의도 동작 복구에 필요하지 않은 API contract 변경.
- 보고된 버그와 무관한 UI behavior 변경.
- 수정에 필요하고 관례에 맞지 않는 DB schema 변경.
- 명확한 증거와 검증 없이 permission, auth, billing, privacy, security, data-loss behavior 변경.
- 표준 workflow 없이 generated, vendor, lock, build output 파일 수정.
- 재현 또는 동등 regression case 검증 없이 버그 수정 주장.
- 실행하지 않은 테스트 통과 주장.

## 워크플로: Reproduce → Root Cause → Regression Test → Minimal Fix → Verify

### 1. 증상 포착

편집 전 정확한 증상을 기록한다.

- 사용자 보고 이슈
- expected behavior
- actual behavior
- error message
- stack trace
- failing command 또는 failing test
- logs 또는 trace IDs
- screenshot 또는 UI state
- API 관련 request payload/response
- input data/output data
- 환경 가정
- deterministic/intermittent/environment-specific 여부
- 과거 동작하던 regression 인지 여부

무엇을 고치는지 이해하기 전에 편집하지 않는다.

### 2. 컨텍스트 탐색

재현과 수정을 위해 충분한 저장소 컨텍스트를 확인한다.

- `AGENTS.md`, `CLAUDE.md`, `.cursor/rules`, `README.md`, `CONTRIBUTING.md`
- `package.json`, `pyproject.toml`, `Cargo.toml`, `go.mod`, `pom.xml`, `build.gradle`, `Makefile`, `justfile`, `Taskfile.yml`
- `jest.config.*`, `vitest.config.*`, `pytest.ini`, `tox.ini`, `playwright.config.*`, `cypress.config.*`, `rspec`, `phpunit.xml`, `.github/workflows`
- 기존 failing tests
- 관련 source/test files
- 가능한 경우 git history 의 최근 변경
- logs, stack traces, screenshots, traces, request payloads, config, env vars, feature flags, seed data, timezone, locale, browser/runtime version, external services
- 유사하게 동작하는 코드 경로와 유사 bugfix
- 영향을 받을 수 있는 UI, API, service, domain, persistence, async jobs, queues, cache, config, auth, permissions, logging, metrics, docs

새 패턴보다 기존 관례를 우선한다.

### 3. 재현

버그 재현을 시도한다.

우선순위:

1. 기존 failing test
2. 사용자 제공 failing command
3. 최소 automated test
4. 최소 script 또는 fixture
5. manual reproduction steps
6. live reproduction 이 불가능할 때 log/trace 기반 재현

기록한다.

- 재현 명령 또는 단계
- 결과
- 실패가 재현됐는지
- 재현되지 않았다면 의심 버그를 지지하는 증거

재현이 불가능하면 명확한 evidence trail 이 있을 때만 진행하고 불확실성을 보고한다.

### 4. Expected behavior 정의

가장 강한 증거로 의도 동작을 정의한다.

- 사용자 요청
- 기존 테스트
- 문서
- 유사 정상 기능
- API contract
- product copy
- type definitions
- 가능한 경우 historical behavior
- domain invariants
- error-handling conventions

expected behavior 가 제품 정책, 보안, billing, privacy, data loss, permission, user-visible semantics 에 영향을 주며 모호하면 중단하고 질문한다.

### 5. Root cause localization

편집 전 root cause 또는 가장 좁은 credible cause 를 식별한다.

- symptom 에서 application code 로 추적
- data flow/control flow 추적
- project code 에 가장 가까운 stack frame 확인
- broken path 와 similar working path 비교
- null, undefined, empty, zero, timezone, locale, encoding, rounding, pagination, sorting, concurrency, race, cache, retry, permission, feature flag 같은 boundary 확인
- git history 가 유용하면 regression 을 만든 최근 변경 확인
- root cause 와 downstream symptom 구분
- 수정 전 원인을 한 문장으로 진술

무작위 patch 금지. 여러 무관 영역을 동시에 고쳐서 맞추려 하지 않는다.

### 6. Regression test

가능하면 프로덕션 코드 변경 전에 regression test 를 추가하거나 갱신한다.

테스트는 다음을 만족해야 한다.

- 수정 전 실패한다.
- bad setup 이 아니라 보고된 버그 때문에 실패한다.
- 가장 작은 의미 있는 실패 case 를 표현한다.
- 의도 동작을 보호한다.
- 구현 세부에 과도하게 맞추지 않는다.
- 유사 테스트 근처에 둔다.
- 기존 helper, fixture, mock, naming convention 을 사용한다.
- 실패를 만든 input, boundary, state 를 커버한다.
- root cause 에 직접 연결된 edge case 만 추가한다.

가능하면 수정 전 regression test 를 실행해 red state 를 확인한다. 불가능하면 이유와 가장 가까운 검증 방법을 설명한다.

### 7. Fix strategy

root cause 를 해결하는 가장 작은 안전 수정을 선택한다.

- 증상만이 아니라 원인을 고친다.
- 무관 동작을 보존한다.
- broad rewrite 보다 local change 를 우선한다.
- 기존 abstraction/convention 을 우선한다.
- opportunistic refactoring 을 피한다.
- speculative generalization 을 피한다.
- 의도 동작 복구에 필요하지 않으면 public contract 를 바꾸지 않는다.
- 테스트 통과를 위해 validation, auth, permission, error handling 을 약화하지 않는다.
- 더 큰 design change 가 필요하면 중단하고 이유를 보고한다.

### 8. Minimal fix implementation

필요한 프로덕션 변경만 구현한다.

- diff 를 좁고 reviewable 하게 유지한다.
- 무관 파일을 건드리지 않는다.
- formatting-only churn 을 피한다.
- 테스트 변경은 regression coverage 에 집중한다.
- 문서는 bugfix 가 문서화된 동작을 바꾸거나 잘못된 문서를 고칠 때만 수정한다.
- generated 파일은 표준 생성 명령으로만 갱신한다.

### 9. Verification

수정 뒤 확인한다.

- regression test 가 통과한다.
- 원래 failing command/test/scenario 가 통과한다.
- 관련 테스트가 계속 통과한다.
- 관련 있고 가능한 경우 typecheck, lint, build, formatting check 가 통과한다.
- UI/API behavior 는 프로젝트의 적절한 메커니즘으로 확인한다.
- root cause 가 class of failures 를 암시하면 similar path 도 깨지지 않았는지 확인한다.

가장 좁고 유용한 테스트부터 실행한 뒤 넓힌다. 실제 검증 없이 성공을 주장하지 않는다.

### 10. Final review

보고 전 final diff 를 확인한다.

- 수정이 root cause 를 직접 해결했는가?
- 무관 동작이 바뀌었는가?
- public API shape 이 바뀌었는가?
- error message/status code 가 의도치 않게 바뀌었는가?
- permission, security, privacy, billing, data-loss behavior 가 바뀌었는가?
- 무관 refactor/cleanup 이 들어갔는가?
- 가능할 때 regression test 가 fix 전 실패하고 fix 후 통과하는가?
- 모든 검증 주장이 실제 실행 명령으로 뒷받침되는가?

### 11. Final report

포함한다.

- 증상
- 재현 방법
- expected behavior
- actual behavior
- root cause
- fix summary
- 추가/갱신한 regression test
- 검증 명령과 결과
- 실행하지 못한 테스트
- 남은 위험 또는 follow-up
- 재현이나 regression test 가 불완전하면 명시적 불확실성

## Regression test checklist

프로덕션 변경 전:

- 실패 case 식별 또는 생성.
- 실패가 보고된 버그를 표현하는지 확인.
- 테스트를 최소화하되 의미 있게 유지.
- 기존 helper/convention 사용.
- 증거 없이 expected behavior 를 바꾸지 않기.
- red command 와 실패 이유 기록.

수정 뒤:

- regression test 재실행.
- 통과 확인.
- 가능한 경우 원래 failing command/scenario 재실행.
- focused check 가 green 된 뒤 검증 확장.

## Root cause checklist

수정 전 확인:

- 실패 동작을 가장 좁은 credible source 로 추적했다.
- expected behavior 에 증거가 있다.
- 가능한 경우 similar working path 와 비교했다.
- boundary condition 을 고려했다.
- git history 가 유용하면 최근 regression 을 확인했다.
- 계획한 수정이 보이는 증상만이 아니라 원인을 해결한다.

## Verification command strategy

1. 기존 failing test 또는 사용자 제공 failing command
2. red 상태의 새/갱신 regression test
3. fix 후 같은 regression test
4. fix 후 원래 failing scenario
5. 관련 테스트 suite
6. typecheck
7. lint
8. build
9. formatting check
10. 관련 UI/API/integration/e2e checks

lint/typecheck 만으로 버그 수정 증거로 삼지 않는다.

## 결정 규칙

- 버그를 재현할 수 없으면 명확한 evidence trail 로만 진행하고 불확실성을 보고한다.
- expected behavior 가 모호하면 안전할 때만 기존 패턴에서 추론하고, 아니면 질문한다.
- security, privacy, billing, permission, data loss, destructive action 이 관련되면 더 강한 증거와 검증이 필요하다.
- failing test 가 invalid 해 보이면 삭제하지 않는다. 이유를 설명하고 behavior coverage 를 유지할 때만 수정한다.
- root cause 가 broad refactor 를 요구하면 minimal fix 와 이후 refactor 를 분리한다.
- root cause 가 새 제품 동작을 요구하면 `feature-dev`로 재분류하거나 조율한다.
- 외부 서비스, config, environment, data 문제면 적절할 때만 local handling 을 고치고 external dependency 를 보고한다.
- 무관 테스트 실패는 pre-existing failure 와 fix 로 인한 failure 로 구분한다.
- public behavior 가 바뀌면 왜 이전 동작이 잘못이고 새 동작이 의도된 것인지 설명한다.
- generated 파일은 표준 명령으로 갱신한다.
- flaky bug 는 가능한 deterministic test 로 만들고, 기존 패턴이 요구하지 않는 한 sleep 으로 때우지 않는다.

## 실패 처리

- regression test 가 fix 전 실패하지 않으면 테스트가 버그를 커버하지 못할 수 있다.
- fix 후에도 regression test 가 실패하면 성공을 주장하지 않는다.
- fix 가 무관 테스트를 실패시키면 정말 무관한지 조사한다.
- 검증을 완료할 수 없으면 무엇을 했고 무엇을 못 했는지 정확히 보고한다.
- 더 큰 design flaw 가 드러나면 minimal safe fix 이후 중단하거나 큰 변경 필요성을 보고한다.
- root cause 가 여러 개면 보고된 문제를 먼저 고치고 나머지는 별도로 보고한다.
- 버그가 codebase 가 아니라 config, data, external system 에 있으면 증거를 문서화하고 불필요한 코드 변경을 피한다.

## Anti-patterns

- 실패 재현 전 production code 편집.
- root cause 없이 random patch 적용.
- 예외를 catch 하고 무시해서 error masking.
- bugfix 중 broad refactoring.
- 한 diff 에 무관 bugfix 포함.
- 테스트를 약화해 통과시키기.
- failing test 삭제.
- 증거 없이 expected behavior 변경.
- auth, permission, privacy, billing, data-loss behavior 를 가볍게 변경.
- lint/typecheck 통과를 버그 수정 증거로 취급.
- 원래 failing scenario 재실행 없이 원 이슈 해결 주장.
- race 를 이해하지 않고 sleep 추가.
- root cause 가 넓은데 exact input 하나에만 과적합.
- minimal targeted fix 대신 module rewrite.
- regression test 가 자동화되거나 문서화되지 않은 상태로 남기기.

## Final response template

Summary:
- <bug fixed and user-visible impact>

Symptom:
- Expected: <expected behavior>
- Actual: <actual behavior>

Root cause:
- <concise explanation>

Fix:
- <minimal change made>

Files changed:
- `<path>`: <reason>

Regression coverage:
- Added/updated: <test files or cases>
- Red phase: `<command>` → <failed as expected because...>
- Green phase: `<command>` → <passed>

Verification:
- Original failure: `<command or scenario>` → <result>
- Related checks: `<command>` → <result>

Notes:
- <unreproduced aspects, skipped checks, pre-existing failures, risks, or follow-ups>
