---
name: feature-dev
description: 사용자가 코드베이스에 새 기능, API, UI 동작, 명령, 옵션, 워크플로, 제품 동작을 추가, 구현, 생성, 지원, 활성화, 노출, 통합, 구축하라고 요청할 때 사용한다. 이 스킬은 Red → Green → Refactor 루프로 요청 동작을 테스트 가능한 기준으로 바꾸고, 프로덕션 코드보다 먼저 테스트를 작성하거나 갱신하며, 의미 있는 실패를 확인한 뒤, 통과를 위한 최소 변경만 구현하고, green 이후에만 리팩토링하며, 실제 검증 결과를 보고하게 한다. 버그 수정, 동작 보존 리팩토링, 조사만 하는 작업, 문서만 수정하는 작업, 테스트 정리만 하는 작업, 기능에 직접 필요하지 않은 의존성 업그레이드에는 사용하지 않는다.
---

# Feature Dev

## 목적

엄격한 테스트 주도 개발로 새 제품 동작이나 코드 동작을 구현한다.

중심 루프는 Red → Green → Refactor 다.

1. 요청 기능을 테스트 가능한 동작으로 바꾼다.
2. 프로덕션 코드 작성 전에 테스트를 작성하거나 갱신한다.
3. 테스트를 실행해 기대한 이유로 실패하는지 확인한다.
4. 실패한 테스트를 통과시키는 최소 프로덕션 변경만 구현한다.
5. 테스트를 다시 실행해 통과를 확인한다.
6. 테스트가 green 이 된 뒤에만 리팩토링한다.
7. 관련 검증 명령을 실행한다.
8. 무엇을 바꿨고 어떻게 검증했는지 보고한다.

## 이 스킬을 사용할 때

요청의 주된 목적이 새 동작 추가일 때 사용한다.

예시:

- GitHub 로그인 지원 추가
- CSV export 구현
- 설정 페이지 생성
- CLI flag 추가
- API 응답에 필드 노출
- 컴포넌트 dark mode 지원
- 실패한 job retry 메커니즘 구축

## 이 스킬을 사용하지 않을 때

다음이 주목적이면 사용하지 않는다.

- 깨진 동작 수정. `bugfix`를 사용한다.
- 동작을 보존한 구조 개선. `refactoring`을 사용한다.
- 조사만 하는 작업.
- 문서만 수정하는 작업.
- 테스트 정리만 하는 작업.
- 기능에 직접 필요하지 않은 의존성 업그레이드.

기능 개발과 버그 수정 또는 리팩토링이 섞이면 기능 구현 범위를 좁게 유지한다. 기존 깨진 동작이 기능을 막으면 먼저 `bugfix`를 사용하고, 구조 정리는 기능이 동작한 뒤 별도 `refactoring` 작업으로 분리한다.

## 비목표

하지 말아야 할 일:

- 광범위한 무관 리팩토링.
- 무관한 버그 수정.
- 기능에 필요하지 않은 아키텍처 재작성.
- 요청되지 않은 제품 동작 추가.
- 테스트 하네스가 있는데 테스트 생략.
- 실행하지 않은 테스트를 correctness 증거로 취급.
- 수행하지 않은 검증을 주장.
- 요청 기능과 무관한 public behavior 변경.
- 필요한 경우가 아니면 generated, vendor, lock, build output 파일 수정.

## 컨텍스트 탐색

코드를 바꾸기 전에 저장소 관례를 확인한다.

있으면 확인한다.

- `AGENTS.md`, `CLAUDE.md`, `.cursor/rules`, `README.md`, `CONTRIBUTING.md`
- `package.json`, `pyproject.toml`, `Cargo.toml`, `go.mod`, `pom.xml`, `build.gradle`, `Makefile`, `justfile`, `Taskfile.yml`
- `jest.config.*`, `vitest.config.*`, `pytest.ini`, `tox.ini`, `playwright.config.*`, `cypress.config.*`, `rspec`, `phpunit.xml`, `.github/workflows`
- 기존 테스트 디렉터리와 naming convention
- 유사 기능과 그 테스트
- 테스트 helper, factory, fixture, mock, stub, snapshot, integration utility
- 영향을 받을 수 있는 UI, API, service, domain, persistence, config, auth, permission, logging, metrics, docs 계층

새 패턴을 만들기보다 기존 관례를 우선한다.

## 워크플로

### 1. 기능 프레이밍

테스트 작성 전에 요청을 구체적 동작으로 번역한다.

식별한다.

- 새 동작
- actor 또는 사용자
- 입력과 출력
- 상태 변화
- 에러 케이스와 edge case
- 권한/인가 영향
- 데이터 저장 영향
- API contract 영향
- UI 상태
- backward compatibility
- 로그/메트릭 같은 observability 필요성

각 acceptance criterion 은 테스트 가능해야 한다. 제품 결정이 비어 있고 public behavior, 보안, privacy, data loss, billing, permission 에 영향을 주면 질문한다.

### 2. 테스트 탐색

새 동작을 설명할 테스트 위치를 찾는다.

- 대상 코드 주변 테스트
- 유사 기능 테스트
- helper, factory, fixture, mock, stub, snapshot, integration utility
- targeted test 명령
- 이 계층에서 선호하는 unit, integration, component, e2e, contract, snapshot 테스트 방식

### 3. 테스트 설계

프로덕션 코드 전에 테스트를 설계한다.

필요에 따라 포함한다.

- happy path
- 중요한 edge case
- validation error
- permission/authorization failure
- empty/loading/failure state
- API request/response shape
- persistence side effect
- event emission
- 프로젝트가 이미 테스트하는 경우에만 logs/metrics
- backward compatibility

구현 세부보다 사용자나 외부 호출자가 관찰하는 동작을 테스트한다.

### 4. Red phase

테스트를 먼저 작성하거나 갱신한다.

그 다음 가장 좁고 유용한 테스트 명령을 실행하고 확인한다.

- 테스트가 실패한다.
- 실패 이유가 기대한 이유다.
- 실패가 요청 동작의 부재 또는 미완성을 증명한다.

구현 전 테스트가 통과하면 테스트를 고치거나 기능이 이미 있는지 재평가한다. 무관한 이유로 실패하면 설정을 바로잡거나 blocker 로 보고한다.

기록한다.

- 실행 명령
- 실패 요약
- 실패가 의미 있는 이유

### 5. Green phase

실패한 테스트를 통과시키는 최소 프로덕션 변경만 구현한다.

규칙:

- 기존 관례를 따른다.
- 적절하면 기존 abstraction 을 재사용한다.
- 요청 동작에만 집중한다.
- 광범위한 리팩토링을 피한다.
- 무관한 public behavior 를 바꾸지 않는다.
- 기능에 직접 필요한 경우에만 config, migration, docs, example 을 추가한다.
- generated 파일은 표준 생성 명령으로만 갱신한다.
- lock 파일은 기능에 필요한 의존성 변경 때만 갱신한다.

targeted test 를 다시 실행해 통과를 확인한다.

### 6. Refactor phase

targeted test 가 green 인 뒤에만 리팩토링한다.

허용되는 리팩토링:

- 구현 중 생긴 중복 제거
- 명확성을 직접 높이는 이름/추출 개선
- 기존 local pattern 과 정렬

범위가 커지면 중단하고 별도 리팩토링 작업을 권한다. 리팩토링 뒤 같은 테스트를 다시 실행한다.

### 7. 더 넓은 검증

targeted test 가 통과한 뒤 가능한 범위에서 검증을 넓힌다.

- 관련 테스트 suite
- typecheck
- lint
- build
- formatting check
- API contract tests
- UI/component/e2e tests
- 자동화 coverage 가 없을 때 manual verification

가장 좁고 유용한 명령부터 실행한 뒤 넓힌다. 비용이 큰 검증은 실행한 것과 생략한 것을 명확히 보고한다.

## 결정 규칙

- 테스트 하네스가 없으면 저장소에 맞는 가장 작은 실용 테스트 하네스만 만든다. 어렵다면 가장 가까운 검증을 수행하고 한계를 보고한다.
- 기능이 이미 있으면 중복 구현하지 않는다. 유용한 경우 테스트나 문서만 보강한다.
- 제품 결정이 필요하면 기존 패턴에서 안전하게 추론하거나 질문한다.
- 보안, privacy, data loss, billing, permission 이 불명확하면 중단하고 질문한다.
- migration 이 필요하면 기존 migration 패턴과 compatibility/rollback 관례를 따른다.
- generated 파일은 표준 생성 명령으로만 갱신한다.
- 무관한 테스트 실패는 기능 변경으로 인한 실패와 분리한다.

## 최종 보고

실제로 수행한 검증만 보고한다.

포함한다.

- 새 동작 요약
- 변경 파일
- 추가/갱신한 테스트
- Red phase 결과: 명령과 기대 실패 요약
- Green phase 결과: 명령과 통과 결과
- 추가 검증 명령과 결과
- 한계, 위험, 생략한 검사, follow-up

실행하지 않은 테스트를 통과했다고 말하지 않는다.

## 품질 기준

완료된 기능은 다음을 만족해야 한다.

- 명시한 acceptance criteria 를 충족한다.
- 테스트 하네스가 있으면 적절한 계층에서 테스트된다.
- 무관한 public behavior 를 보존한다.
- local convention 을 따른다.
- 구현 범위가 좁다.
- 다른 엔지니어가 확인할 수 있는 검증 증거를 남긴다.
