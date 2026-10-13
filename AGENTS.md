# 에이전트 지침

목차를 확인하고, 필요한 지침만 읽어라.

## 목차

1. Spring / Java
2. Test
3. Commit Convention
4. Documentation

## Spring / Java

- FQCN을 코드에 직접 사용하지 말고, import 후 단순 클래스명을 사용해라.
- Java 테스트 코드에서는 given, when, then, given & when 등의 주석 패턴을 사용해라.

## Test

- 테스트는 단위 테스트와 최종 E2E 테스트만 진행한다. 통합 테스트는 진행하지 않는다.

## Commit Convention

- AngularJS commit message rule과 한국어를 사용해라.

## Documentation

- 내가 중요한 의사결정을 내리면, 저장 여부를 따로 묻지 말고 항상 해당 내용을 `docs/decisions/`에 기록해.
- 유사한 의사결정 문서가 있으면 해당 문서에다가 추가해. 수정이나 삭제 금지(append-only).
