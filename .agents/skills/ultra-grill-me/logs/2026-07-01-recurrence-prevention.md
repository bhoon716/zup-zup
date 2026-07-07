# 운영 디스크 포화 재발 방지 Grill 세션

- 날짜: 2026-07-01
- 케이스: implementation-plan-grill
- 상태: 진행 중

## 요약

운영 디스크 포화 재발 방지를 위해 근본 개선 수준으로 진행하기로 했고, 1순위는 저장소 분리로 정했으며, 이제 첫 분리 대상을 좁히는 단계입니다.

## 질문 로그

### Q1. 이번 재발 방지의 1차 완료 기준은 어디까지로 둘까요?

- 막힌 결정: 재발 방지 작업의 Done 기준이 정해지지 않아, 어디까지를 1차 조치로 볼지 결정할 수 없었음
- 답변: 3번. 근본 개선
- 결과: 확정

### Q2. 근본 개선에서 무엇을 1순위로 둘까요?

- 막힌 결정: 근본 개선의 시작점이 정해지지 않아 작업 순서를 만들 수 없었음
- 답변: 추천
- 결과: 확정

### Q3. 저장소 분리의 첫 대상은 무엇으로 할까요?

- 막힌 결정: 저장소 분리를 아무 순서로나 하면 효과가 분산되므로, 이번 장애의 직접 원인에 가장 먼저 대응할 대상을 정해야 했음
- 답변: 1. MySQL 데이터 볼륨부터 분리
- 결과: 확정

### Q4. MySQL 저장소 분리 범위는 어디까지로 할까요?

- 막힌 결정: MySQL 전체를 별도 볼륨/디스크로 뺄지, binlog만 따로 뺄지, 아니면 둘 다를 분리할지 정해야 실제 아키텍처가 결정됨
- 답변: 1. MySQL `datadir` 전체를 별도 볼륨/디스크로 분리
- 결과: 확정

### Q5. MySQL `datadir`를 어디에 둘까요?

- 막힌 결정: Docker 볼륨만 따로 두면 여전히 같은 루트 디스크를 공유할 수 있어서, 실제로 재발 방지 효과가 충분한지 판단해야 했음
- 답변: 2. Docker named volume만 별도
- 결과: 확정

### Q6. `binlog` 보존 기간을 어떻게 둘까요?

- 막힌 결정: 복제/PITR를 지금 쓰지 않더라도 나중 가능성을 남길지, 디스크 리스크를 줄이기 위해 짧게 고정할지 정해야 했음
- 답변: 복제/PITR를 나중에 쓸 가능성은 있지만 지금은 없으므로 `binlog`를 유지하되 보존 기간을 1~3일로 짧게 둔다
- 결과: 확정

## 확정된 결정

- 재발 방지 수준은 근본 개선으로 간다.
- 1순위는 저장소 분리다.
- 첫 분리 대상은 MySQL 데이터 볼륨이다.
- MySQL 저장소 분리 범위는 `datadir` 전체다.
- MySQL `datadir`는 Docker named volume으로 둔다.
- `binlog`는 짧게 유지한다.

## 임시 가정

- Docker named volume은 루트 디스크 공유 리스크를 감수하는 임시 선택이다.

## 남은 known unknowns

- `binlog` 보존 기간을 1일, 2일, 3일 중 어디로 고정할지

## 2026-07-01T00:00:00Z - 진행 중

- 현재 상태:
  - MySQL `datadir`는 Docker named volume으로 유지한다.
  - `binlog`는 짧게 유지한다.
  - 정확한 보존 일수만 남아 있다.
- 다음 질문:
  - `binlog` 보존 기간을 1일, 2일, 3일 중 어디로 고정할지
- 추천 기준:
  - 2일은 최근 장애 추적에 충분한 최소 여유를 남기면서도 디스크 리스크를 크게 키우지 않는 균형점이다.

### Q7. `binlog` 보존 기간을 몇 일로 고정할까요?

- 막힌 결정: exact retention day가 정해져야 운영 정책과 디스크 여유를 확정할 수 있었음
- 답변: 2
- 결과: 확정

## 2026-07-01T00:00:00Z

- Notes:
  - Replication is primarily a high-availability tool, not a backup strategy.
  - PITR is only justified if the service needs recovery to a point between scheduled backups.
  - Current project docs describe a single-host self-hosted deployment and explicitly keep future off-host disaster recovery as follow-up work, so replication is not a current architectural requirement.
  - The user accepted a short binlog retention window (1-3 days) instead of long retention.

## 최종 정리

### 1. 정리된 방향

운영 서버의 재발 방지는 저장소 분리와 보존 정책 정리로 간다. MySQL `datadir`는 Docker named volume으로 유지하고, `binlog`는 2일만 보존한다.

### 2. 확정된 결정

- 재발 방지 수준은 근본 개선으로 간다.
- 1순위는 저장소 분리다.
- 첫 분리 대상은 MySQL 데이터 볼륨이다.
- MySQL 저장소 분리 범위는 `datadir` 전체다.
- MySQL `datadir`는 Docker named volume으로 둔다.
- `binlog` 보존 기간은 2일로 고정한다.

### 3. 아직 남은 known unknowns

- 장기적으로 replication/PITR를 실제로 도입할지 여부
- `binlog`를 2일로 둔 뒤에도 실제 디스크 사용량이 얼마나 안정적으로 유지되는지

### 4. 핵심 가정

- 현재 서비스는 단일 호스트 운영이므로, 복제는 필수 요구가 아니다.
- PITR는 현재의 운영 요구사항이 아니라 향후 선택지다.
- 2일 보존이면 최근 장애 추적에 충분한 여유를 주면서 디스크 리스크를 과도하게 키우지 않는다.

### 5. 가장 큰 리스크

- `binlog` retention이 2일이어도 다른 로그나 캐시가 다시 같이 불어나면 루트 디스크가 재포화될 수 있다.

### 6. 주요 tradeoff

- 더 짧게 두면 디스크는 안전해지지만 추적 여유가 줄어든다.
- 더 길게 두면 추적 여유는 늘지만, 이번 장애의 핵심 원인인 디스크 압박이 다시 커진다.

### 7. 하지 않기로 한 것

- `binlog` 장기 보존
- 지금 단계에서 replication/PITR 도입
- root disk를 그대로 방치한 채 retention만 늘리는 방향

### 8. 가장 작은 다음 행동

- 운영 MySQL 설정에서 `binlog` 자동 정리 기준을 2일로 반영할지, 그리고 그 설정이 실제로 적용되는 배포 절차를 정한다.

### 9. 왜 여기서 멈춰도 되는지

- 이번 grill의 핵심 불확실성은 `binlog` 보존 기간이었고, 그 값이 2일로 확정됐다.
- 남은 항목은 실행 단계의 반영 문제라서, 설계 판단 자체는 더 이상 막히지 않는다.
