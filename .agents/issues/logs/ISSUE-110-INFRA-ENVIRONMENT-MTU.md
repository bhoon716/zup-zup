# ISSUE-110 verification log

- 2026-07-14: Compose의 `com.docker.network.driver.mtu`가 9000으로 고정되어 있고 로컬/운영 override가 없음을 확인했다.
- 2026-07-14: `DOCKER_NETWORK_MTU`를 도입해 로컬 1500·OCI 9000을 분리하고, 1400 입력 거부 및 양쪽 config 출력 검증을 통과했다.
