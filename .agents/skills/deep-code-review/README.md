# Deep Code Review

<p align="center">
  <a href="./README.md">English</a> | <a href="./README.ko.md">한국어</a> | <a href="./README.zh.md">简体中文</a>
</p>

`deep-code-review`는 구체적인 PR, diff, commit, patch, branch 또는 변경 파일을 읽기 전용으로 여러 전문 관점에서 리뷰하는 오케스트레이션 스킬입니다.

## 핵심 파이프라인

1. 리뷰 범위를 확정합니다.
2. 적용되는 저장소 지침을 수집합니다.
3. 변경 내용을 분류합니다.
4. 필수·조건부 review lens를 선택합니다.
5. 격리된 전문 reviewer를 실행합니다.
6. dynamic escalation을 처리합니다.
7. 모든 candidate finding을 독립 검증합니다.
8. root cause 기준으로 중복 제거 후 confirmed finding만 합성합니다.

runtime 변경에는 `correctness`와 `contract-tests`를 필수 실행하고, security·reliability·architecture·infrastructure는 trigger에 따라 추가합니다.

## 설치

```bash
skill-forge install deep-code-review --lang ko --agent codex
```

v0 전문 지식은 `SKILL.md`와 `references/` 아래 9개 파일에 있습니다. 결과에는 confirmed finding, coverage, limitation만 포함합니다. 최종 report는 합성 완료 후 `.agents/reviews/deep-code-review/<review-id>.md`에 생성하며 중간 reviewer 결과는 저장하지 않습니다.
