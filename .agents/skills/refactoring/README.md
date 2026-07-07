# Refactoring

<p align="center">
  <a href="./README.md">English</a> | <a href="./README.ko.md">한국어</a> | <a href="./README.zh.md">简体中文</a>
</p>

`refactoring`은 외부에서 관찰 가능한 동작을 보존하면서 코드 구조를 개선하게 하는 코딩 에이전트 스킬입니다.

핵심 루프는 **Baseline → Transform → Same Tests**입니다.

## 사용할 때

- 동작 변경 없이 코드 단순화
- helper, function, module, component 추출
- 중복 제거
- 이름, 가독성, 응집도, 관심사 분리 개선
- 동작 변경 없이 테스트하기 쉽게 만들기

## 사용하지 않을 때

- 새 동작 추가. `feature-dev` 사용
- 깨진 동작 수정. `bugfix` 사용
- 광범위한 rewrite 또는 architecture redesign
- 의존성 업그레이드
- 테스트나 동등 검증으로 보호할 수 없는 cleanup

## 워크플로

1. 정확한 리팩토링 목적을 밝힌다.
2. 바뀌면 안 되는 동작 경계를 정의한다.
3. 대상 동작을 보호하는 테스트를 찾는다.
4. 편집 전 baseline tests 를 실행한다.
5. 작은 변환 하나를 적용한다.
6. 같은 테스트를 다시 실행한다.
7. 같은 테스트가 통과하는 동안만 계속한다.
8. 동작 보존을 검증할 수 없으면 중단, revert, 또는 scope 축소한다.

## 설치

```bash
skill-forge install refactoring --lang en --agent codex
skill-forge install refactoring --lang ko --agent codex
skill-forge install refactoring --lang zh --agent codex
```

## 프로젝트 힌트

하위 프로젝트의 `AGENTS.md`에 필요하면 추가합니다.

> 사용자가 동작 변경 없이 refactor, clean up, simplify, extract, rename, duplication 제거를 요청하면 `refactoring`을 사용한다. 먼저 baseline tests 를 실행하고, 작은 변환 하나를 적용한 뒤, 계속하기 전에 같은 테스트를 다시 실행한다.
