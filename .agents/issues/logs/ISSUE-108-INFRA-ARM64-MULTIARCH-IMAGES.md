# ISSUE-108 verification log

- 2026-07-14: M1 Docker에서 Loki/Grafana/Prometheus/Promtail/Nginx Proxy Manager 고정 digest가 AMD64 단일 플랫폼임을 확인했다.
- 2026-07-14: 동일 릴리스의 멀티아키텍처 manifest digest로 교체하고 `docker buildx imagetools inspect`에서 관련 이미지의 `linux/arm64` manifest를 확인했다. Compose 정책 검증이 통과했다.
