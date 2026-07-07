# 레포지토리 점수판 (Repository Scorecard)

본 문서에는 감사 결과 도출된 영역별 소프트웨어 품질 평가 등급과 그 근거를 상술합니다.

## 종합 레이더 차트

```mermaid
radar-chart
    title "종합 품질 진단 차트"
    axes
        "Architecture (아키텍처)": 8
        "Security (보안)": 2
        "Performance (성능)": 7
        "Maintainability (유지보수성)": 8
        "Scalability (확장성)": 6
        "Reliability (안정성)": 7
        "Testability (테스트 용이성)": 9
        "Readability (가독성)": 9
        "Observability (관측성)": 8
```

## 세부 상세 진단 보고

### 1. 아키텍처 (Architecture): 8/10
* **설명**: 클린 아키텍처 레이어 규격이 정상 작동하며 비즈니스 규칙이 강하게 응집됨.
* **보완점**: JWT 보안 필터링 규칙이 HTTP Session 영역과 오염 결합된 상태를 정비해야 함.

### 2. 보안 (Security): 2/10
* **설명**: 보안 시크릿 자산이 다수 노출되어 즉시 보완 조치가 시급함.
* **보완점**: 노출 갱신 처리 및 플레이스홀더를 활용한 동적 바인딩 이식.

### 3. 성능 (Performance): 7/10
* **설명**: QueryDSL 슬라이스 처리를 통하여 대량 목록 조회를 제어함.
* **보완점**: 빈번한 복합 조건 검색 조인을 위한 복합 인덱스 부재 보정 필요.

### 4. 유지보수성 (Maintainability): 8/10
* **설명**: 훅, 유틸리티, 서비스 레이어가 깔끔함.
* **보완점**: 알림 전송 레이어의 발송 채널 결합도 정리를 위한 전략 패턴 적용.

### 5. 테스트 용이성 (Testability): 9/10
* **설명**: Vitest 및 MockMvc 테스트 빌드 규격이 가장 탄탄하게 선언되어 유지됨.

### 6. 관측 가능성 (Observability): 8/10
* **설명**: Loki, Promtail, Grafana 등 통합 인프라 스택 구축 완료 상태.
