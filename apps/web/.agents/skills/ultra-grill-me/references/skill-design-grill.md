# Skill 설계 검증

## 이 reference를 사용할 때

- Agent Skill 설계를 검증해달라는 요청
- SKILL.md 구조를 만들기 전에 질문으로 검증해달라는 요청
- trigger 규칙이 맞는지 확인해달라는 요청
- reference 구조가 적절한지 grill 해달라는 요청
- eval 설계를 검토해달라는 요청
- Skill의 scope가 적절한지 확인해달라는 요청

## 이 reference를 사용하지 않을 때

- Skill이 뭔지 설명해달라는 요청
- SKILL.md를 바로 작성해달라는 요청
- 프롬프트를 교정해달라는 요청
- 일반적인 프롬프트 엔지니어링 조언을 구하는 요청

## 질문 우선순위

1. Skill로 만들 만한 반복 작업인지
2. trigger 조건 — 어떤 요청에서 켜져야 하는지
3. non-trigger 조건 — 어떤 요청에서 켜지면 안 되는지
4. workflow 관찰 가능성 — 각 단계가 테스트 가능한지
5. output format — 결과물의 구조가 정해져 있는지
6. reference selection — 어떤 reference를 언제 읽을지 명시되어 있는지
7. stopping conditions — 언제 멈출지 기준이 있는지
8. gotchas — 자주 실패하는 지점이 정리되어 있는지
9. eval case — 테스트 케이스를 만들 수 있는지
10. release gate — 배포 기준이 있는지

## 강한 질문 패턴

- "이 Skill이 반복적으로 처리하는 작업이 정확히 뭔가요? 한 문장으로 말해주세요."
- "이 Skill이 켜지면 안 되는 요청을 3개만 들어주세요."
- "workflow의 각 단계를 외부에서 관찰할 수 있나요? 예를 들어 '질문을 하나만 했는지' 같은 건 확인 가능한가요?"
- "이 Skill의 output format 없이도 결과물이 일관될까요?"
- "description이 너무 넓어서 다른 Skill과 trigger가 겹치지 않나요?"

## 약한 질문 패턴

- "이 Skill 괜찮아 보이나요?"
- "뭔가 빠진 거 없나요?"
- "더 좋게 만들 수 있나요?"

## 추천 옵션 구성 규칙

- **Skill scope가 불분명할 때**: `(추천) 하나의 좁고 반복 가능한 핵심 워크플로에 집중` 옵션을 추천하고, 넓은 기능 범위를 가진 대안들을 포함하여 선택지 구성
- **trigger 조건이 모호할 때**: `(추천) Use when [명확한 진입상황]. Do not use for [제외상황].` 템플릿 옵션을 추천하고, 광범위한 자연어 trigger 설정 대안들로 선택지 구성
- **output format이 없을 때**: `(추천) 최소 3개 필수 섹션 (요약, 핵심 발견, 다음 행동)` 구조 옵션을 추천하고, 자유 형식 출력 대안들로 선택지 구성


## 모호한 답변 처리

- "아직 정확히 모르겠어" → "그럼 이 Skill이 처리하는 작업을 '사용자의 X를 Y 방식으로 검증하는 것'으로 임시 정의할까요?"로 기본값 제안
- "여러 가지를 다 하고 싶어" → "하나의 Skill이 여러 job을 하면 trigger가 겹치고 테스트가 어려워집니다. 가장 자주 반복되는 하나만 먼저 고르면 어떤 건가요?"로 범위 좁히기
- "나중에 정할게" → assumption으로 기록하고 다음 질문으로 넘어간다

## 케이스별 종료 조건

공통 종료 조건에 더해 다음이 충족되어야 한다.

- Skill의 반복 작업이 한 문장으로 정리되었다
- trigger와 non-trigger가 각각 최소 3개 예시로 구분 가능하다
- workflow 단계가 관찰 가능하다
- output format이 있다
- 최소 1개의 eval case를 만들 수 있다
- 가장 먼저 작성할 파일이 정해졌다

## 최종 정리 필수 항목

공통 최종 정리 형식에 다음을 추가한다.

- 확정된 Skill 범위
- trigger / non-trigger 경계
- workflow
- reference 구조
- eval 설계
- 가장 먼저 작성할 파일

## 실패 모드

- Skill scope를 너무 넓게 잡아서 만능 프롬프트가 되는 경우
- trigger와 non-trigger 경계를 정하지 않은 채 SKILL.md를 작성하기 시작하는 경우
- workflow 단계가 테스트 불가능한 경우
- description이 다른 Skill과 겹치는 경우
- reference를 분리했지만 언제 읽을지 명시하지 않은 경우
- eval 없이 완성이라고 판단하는 경우
