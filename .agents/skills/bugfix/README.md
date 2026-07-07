# Bugfix

<p align="center">
  <a href="./README.md">English</a> | <a href="./README.ko.md">한국어</a> | <a href="./README.zh.md">简体中文</a>
</p>

`bugfix`는 깨진 동작, 잘못된 동작, 실패하거나 flaky 하거나 회귀된 동작을 증거 기반으로 수정하게 하는 코딩 에이전트 스킬입니다.

핵심 루프는 **Reproduce → Root Cause → Regression Test → Minimal Fix → Verify**입니다.

## 사용할 때

- 실패하는 테스트 수정
- crash, stack trace, 잘못된 status code, 무시되는 CLI flag, 중복 output, 잘못된 rendering 수정
- 예전에는 동작했지만 지금은 안 되는 regression 수정
- 사용자가 수정을 원하는 실패 조사

## 사용하지 않을 때

- 새 동작 추가. `feature-dev` 사용
- 동작 보존 cleanup. `refactoring` 사용
- 수정 없는 조사만 하는 작업
- 문서만 수정
- 테스트 정리만 하는 작업
- 버그 수정에 필요하지 않은 의존성 업그레이드

## 워크플로

1. 정확한 증상을 포착한다.
2. 실패를 재현하거나 기존 failing test 를 찾는다.
3. expected behavior 와 actual behavior 를 정의한다.
4. 편집 전에 root cause 를 국소화한다.
5. 가능하면 regression test 를 추가하거나 갱신한다.
6. regression test 가 기대한 이유로 실패하는지 확인한다.
7. 최소 안전 수정을 적용한다.
8. regression test 와 원래 failing scenario 를 다시 실행한다.
9. 관련 검증을 실행하고 불확실성을 정직하게 보고한다.

## 설치

```bash
skill-forge install bugfix --lang en --agent codex
skill-forge install bugfix --lang ko --agent codex
skill-forge install bugfix --lang zh --agent codex
```

## 프로젝트 힌트

하위 프로젝트의 `AGENTS.md`에 필요하면 추가합니다.

> 사용자가 깨진 동작, 실패하는 테스트, flaky 동작, 회귀된 동작을 고치라고 요청하면 `bugfix`를 사용한다. 실패를 재현하고, root cause 를 식별하고, 가능하면 regression test 를 추가/갱신하고, 최소 수정 후 원래 실패가 해결됐는지 검증한다.
