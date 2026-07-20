# [P1][infra] 로컬·운영 환경별 Docker 네트워크 MTU 분리

## 문제

Compose가 Docker bridge MTU를 `9000`으로 하드코딩한다. 이는 OCI 호스트의 특정 네트워크 조건을 반영한 값이며 Docker Desktop Mac의 기본 MTU 1500과 다르다. 같은 Compose를 로컬에서 실행하면 연결 실패·패킷 단편화·재현성 차이가 생길 수 있다.

## 완료 기준

- MTU를 환경 변수 또는 명시적인 Compose override로 분리한다.
- 로컬 기본값은 1500으로 두고 운영에서만 검증된 9000을 사용한다.
- `.env.example`와 운영 문서에 선택 기준과 검증 명령을 기록한다.
- 잘못된 MTU 값이 배포 전에 거부된다.

## 근거

- `infra/docker-compose.yml`
- `docs/troubleshooting.md`의 MTU 해결 기록
- `infra/.env.example`
