# [P1][infra] 로컬 Compose 환경변수 누락 사전 검증

## 문제

`infra`에서 `docker compose up -d`를 실행하면 `DB_JDBC_URL`, DB 계정·비밀번호, `ALERTMANAGER_WEBHOOK_URL` 등이 설정되지 않았다는 경고가 출력된다. Compose는 누락된 보간 변수를 빈 문자열로 대체하므로, 실행자가 `.env`를 준비하지 않았거나 잘못된 값을 넣은 상태를 시작 전에 알 수 없다.

## 완료 기준

- 필수 보간 변수가 없거나 빈 값이면 Compose 실행 전 명확한 오류를 낸다.
- `infra/.env.example`을 복사해 사용할 표준 로컬 설정 절차를 문서화한다.
- 비밀값과 선택값의 구분을 문서에 명시하고, 실제 비밀값을 저장소에 넣지 않는다.
- 누락 변수와 빈 문자열 치환을 잡는 정책 또는 회귀 검증을 추가한다.

## 재현 증거

```text
cd infra
docker compose up -d
WARN The "DB_MIGRATOR_PASSWORD" variable is not set. Defaulting to a blank string.
WARN The "DB_JDBC_URL" variable is not set. Defaulting to a blank string.
...
```

## 관련 파일

- `infra/docker-compose.yml`
- `infra/.env.example`
- `infra/README.md`
- `infra/scripts/verify-compose-policy.sh`
