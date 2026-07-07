# 인프라 진단 결과 (Infrastructure Findings)

## 1. Promtail 컨테이너 과다 권한 볼륨 마운트 (INFRA-001)
* **위치**: [docker-compose.yml](file:///Users/bhoon/Project/jbnu-sugang-helper/infra/docker-compose.yml#L114)
* **상세**: Promtail 컨테이너가 호스트의 시스템 전체 로그를 수집하기 위해 `/var/log` 전체 경로를 일대일 마운트해 두고 있습니다.
* **영향**: Promtail 컨테이너 탈취 시 호스트 OS의 권한(인가 실패 기록, 보안 로그 등)이 완전히 노출되는 심각한 호스트 침해 위협이 됩니다.
* **조치 제안**: 수강신청 헬퍼 서비스 및 관련 프록시 로그만 마운트 대상으로 한정해 마운트 권한의 범위와 노출 면적을 최소화하십시오. (예: `/var/log/jbnu-sugang-helper:/var/log/jbnu-sugang-helper:ro`)

## 2. Docker Compose 내 리소스 제어 한계(Limits) 부재
* **위치**: 모든 서비스 컨테이너 정의 영역
* **상세**: `docker-compose.yml` 내 서비스 정의에 CPU 및 메모리 제한 사양이 정의되어 있지 않습니다.
* **영향**: 크롤러 배치 작업 동작 시 비정상적으로 메모리를 과다 점유하게 되면 단일 호스트 인프라 전체가 마비(OOM Crash)될 우려가 있습니다.
* **조치 제안**: 각 서비스에 컨테이너 수준 리소스 보장(CPU 0.5 core, MEM 512M 수준) 한계 설정을 기재해 보호하십시오.
