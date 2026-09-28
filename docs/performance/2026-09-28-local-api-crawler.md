# 로컬 API·크롤러 부하 테스트 smoke — 2026-09-28

## 목적과 범위

로컬 k6 스크립트가 과목 검색 API에 연결되고, 실제 과목 데이터가 있는 정상 응답을 확인하는 1 VU smoke다. 처리 용량을 정하거나 운영 peak를 재현한 결과가 아니다. 15분 기준선·단계 부하는 아직 실행하지 않았다.

## 실행 조건

- 앱 revision: `9e81c0a0`
- 환경: Docker Desktop ARM64, 앱·MySQL·Redis는 Docker VM의 CPU 0을 공유; k6는 Mac 호스트에서 실행
- DB: 전용 로컬 볼륨, migration v28; 현재 2026년 2학기 설정으로 실제 JUMP 정기 크롤링 5,026건 적재
- 요청: `POST /api/v1/courses/search?page=0&size=30`, 2026년 2학기(`U211600020`), 인기순
- k6: 1 VU, 1분, iteration 사이 1초 대기; 56회 요청
- 부수효과: 실제 크롤러는 기존 5분 주기, Email/Discord/Web Push/FCM/AWS SES off
- k6 summary JSON: [2026-09-28-local-api-crawler-k6.json](2026-09-28-local-api-crawler-k6.json)

## 결과

| 측정값 | 결과 |
| --- | ---: |
| HTTP 요청 | 56 (0.933 req/s) |
| HTTP 실패 | 0 / 56 |
| k6 checks | 280 / 280 통과 |
| 요청 지연 평균 | 70.1 ms |
| 요청 지연 중앙값 | 69.3 ms |
| 요청 지연 P95 | 84.1 ms |
| 요청 지연 최댓값 | 107.3 ms |

## 해석과 한계

로컬 주소 제한, 검색 API 응답 검사, 과목 데이터 적재, 컨테이너 기동은 smoke로 확인했다. 최초 실행은 migration이 기본으로 넣은 1학기 데이터였으므로 최종 설정에서는 전용 DB의 대상 학기를 2학기로 바꾸고 다시 실행했다. 단일 VU·짧은 표본이므로 P95는 용량 보장이나 사용자 체감 목표로 볼 수 없다.

알림 채널과 AWS SES는 꺼져 있었고 Firebase 파일을 마운트하지 않아 초기화 실패 로그가 남았지만 readiness는 UP이었다. 따라서 notification delivery나 outbox fan-out은 시험하지 않았다. 크롤러는 평소 5분 주기로 설정되어 있었으나, 이 smoke의 측정 창에서 크롤러 실행과 API 요청이 겹쳤는지는 기록하지 않았다. 동시 실행의 증거로 해석하지 않는다.

새 DB migration에서 Flyway 11.7.2는 MySQL 8.4가 공식 지원 테스트 범위보다 새 버전이라고 경고했지만, 27개 migration validation 및 적용은 v28까지 성공했다. smoke 중 컨테이너 CPU·메모리 시계열은 따로 수집하지 않아 자원 포화 여부도 판단하지 않는다.

## 다음 측정

1. 검색 단독 0.41 req/s 기준선과 단계 부하는 병목 진단이 필요할 때만 실행한다.
2. 혼합 harness를 준비한 뒤, 경로별 API 부하 + 실제 5분 크롤링 + 합성 outbox/fake sink를 최소 3회 크롤링 주기 동안 시험한다.
3. 로컬 upstream stub으로 좌석 변화부터 fake sink 전달까지 전체 경로를 재현하고, provider 지연·크롤러 실패를 주입해 backlog와 복구를 확인한다.
4. 현재 구조와 worker 분리 후보를 같은 입력·요청 비율·총 자원 예산으로 반복 비교한다. 혼합 시험이 끝나기 전에는 이 smoke만으로 분리 여부를 결정하지 않는다.
