# Feature Dev

<p align="center">
  <a href="./README.md">English</a> | <a href="./README.ko.md">한국어</a> | <a href="./README.zh.md">简体中文</a>
</p>

`feature-dev`는 새 제품 동작이나 코드 동작을 엄격한 TDD로 구현하게 하는 코딩 에이전트 스킬입니다.

핵심 루프는 **Red → Green → Refactor**입니다.

## 사용할 때

- 새 API, 명령, 옵션, 워크플로, UI 동작 추가
- API 응답 필드 노출
- export, login, retry, settings, mode 지원 구현
- 기존 저장소에 새 제품 동작 추가

## 사용하지 않을 때

- 버그 수정. `bugfix` 사용
- 동작 보존 cleanup. `refactoring` 사용
- 조사만 하는 작업
- 문서만 수정
- 테스트 정리만 하는 작업
- 기능에 직접 필요하지 않은 의존성 업그레이드

## 워크플로

1. 요청을 테스트 가능한 acceptance criteria 로 바꾼다.
2. 기존 관례, 테스트, helper 를 찾는다.
3. 테스트를 먼저 작성하거나 갱신한다.
4. targeted test 를 실행해 기대한 red 실패를 확인한다.
5. 최소 프로덕션 변경을 구현한다.
6. 같은 테스트를 다시 실행해 green 을 확인한다.
7. green 이후에만 리팩토링한다.
8. 더 넓은 검증을 실행하고 실제 검증한 내용만 보고한다.

## 설치

```bash
skill-forge install feature-dev --lang en --agent codex
skill-forge install feature-dev --lang ko --agent codex
skill-forge install feature-dev --lang zh --agent codex
```

## 프로젝트 힌트

하위 프로젝트의 `AGENTS.md`에 필요하면 추가합니다.

> 사용자가 새 동작을 add, implement, create, support, expose, integrate, build 하라고 요청하면 `feature-dev`를 사용하고 Red → Green → Refactor 를 따른다. 테스트 하네스가 있으면 실패하는 동작 테스트 없이 프로덕션 코드를 먼저 구현하지 않는다.
