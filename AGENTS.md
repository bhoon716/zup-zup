# 에이전트 지침

- [프로젝트 코드 컨벤션](.agents/conventions/README.md)을 항상 따른다.

## pstack

- pstack 스킬을 사용할 때는 [Codex 호스트 매핑](.agents/pstack/CODEX.md)도 읽는다. 사용자가 지정한 스킬을 직접 실행할 수 있다. 이 Codex Desktop의 호출 표기는 `$pstack:poteto-mode`, `$pstack:setup-pstack`, `$pstack:poteto-help`다. 짧은 이름을 제공하는 CLI에서는 `$poteto-mode`처럼 호출한다.
- pstack이 설치된 작업공간에서는 여러 파일이나 호출 계약에 걸친 작업, 설계 선택, 원인이 확인되지 않은 버그, 성능 문제, PR 진행 관리에 `.agents/pstack/skills/poteto-mode/SKILL.md`와 해당 플레이북을 적용한다. 새 체크아웃의 설치 방법은 [Codex 호스트 매핑](.agents/pstack/CODEX.md)에 있다. 단일 파일의 명확한 변경은 직접 검증한다.
- 원본의 검증·독립 리뷰·격리·종료 조건을 지킨다. 사용자가 정한 작업 범위와 현재 도구 권한을 기준으로 외부 쓰기, 배포, 병합, 예약을 실행한다.
