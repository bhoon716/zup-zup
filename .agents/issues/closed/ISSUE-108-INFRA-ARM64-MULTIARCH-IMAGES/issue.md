# [P1][infra] ARM64 네이티브 멀티아키텍처 이미지 digest 정리

## 문제

M1 Mac과 OCI A1은 모두 ARM64인데 Compose의 Loki, Grafana, Prometheus, Promtail, Nginx Proxy Manager 이미지가 AMD64 단일 플랫폼 digest로 고정되어 있다. 로컬에서는 에뮬레이션 경고와 성능 저하가 발생하고, OCI에서는 실행 실패 가능성이 있다.

## 완료 기준

- 현재 릴리스 버전을 유지하면서 ARM64를 포함한 manifest-list/index digest로 교체한다.
- M1과 ARM64 운영 호스트에서 Docker가 ARM64 이미지를 선택하는 것을 확인한다.
- Compose 정책 검증이 단일 플랫폼 digest 또는 ARM64 미포함 이미지를 거부한다.
- ARM64 이미지 정책과 운영 문서가 실제 Compose 설정과 일치한다.

## 근거

- `infra/docker-compose.yml`
- `infra/scripts/verify-compose-policy.sh`
- `docs/troubleshooting.md`의 ARM64 이미지 해결 기록
