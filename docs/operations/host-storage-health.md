# 호스트 저장 공간 점검

`infra/scripts/check-storage-health.py`는 OCI 호스트의 release 및 Docker 데이터 경로가 위치한 파일시스템을 읽기 전용으로 검사한다. Python 3 표준 라이브러리의 `os.statvfs`만 사용하며 Docker 소켓, 네트워크, 파일 내용 조회, 삭제, 설치를 수행하지 않는다.

**현재 상태: 스크립트·systemd 타이머·Alloy/Prometheus 연결 코드와 오프라인 테스트를 준비했다. 운영 서버 실행, 타이머 설치·활성화 및 실제 알림 수신 검증은 하지 않았다. 승인 후 활성화하기 전에는 지속적인 호스트 모니터링이 아니다.** Grafana 대시보드는 Grafana UI에서 관리한다.

## 수동 실행

운영 접근이 승인된 후 OCI 호스트에서 실행한다. 기본 release 경로는 배포 스크립트와 같은 `/home/ubuntu/jbnu-sugang-helper`, 기본 Docker 데이터 경로는 `/var/lib/docker`다. Docker의 실제 data-root가 다르면 운영자가 확인한 경로를 명시한다. 이 스크립트는 Docker 설정을 자동 조회하지 않는다.

```sh
python3 infra/scripts/check-storage-health.py
# 다른 저장소를 사용하는 경우:
python3 infra/scripts/check-storage-health.py \
  --release-path /actual/release/root \
  --docker-data-path /actual/docker/data
```

각 경로는 존재하고 호출 사용자에게 조회 권한이 있어야 한다. 없는 경로는 상위 디렉터리로 대체하지 않으며, 권한을 자동 변경하거나 sudo로 재실행하지 않는다. 두 경로가 같은 파일시스템이면 같은 수치가 두 역할로 출력될 수 있다.

## 기준과 결과

- 기본 최소 여유: **5 GiB 및 전체 블록의 15%**, 최소 가용 inode **10%**. 셋 중 하나라도 미달이면 `low`, 경계값과 같으면 `ok`다. 운영 용량과 증가 속도에 맞춰 조정한다.
- `--min-free-bytes`는 양의 정수, `--min-free-percent`와 `--min-free-inode-percent`는 0 초과 100 이하의 유한한 수다. 0으로 검사를 끌 수 없다.
- `freeBytes`는 `f_bavail * f_frsize`, `freePercent`는 `f_bavail / f_blocks * 100`이다. root 예약 공간을 일반 사용자 가용 공간으로 취급하지 않는다. inode도 `f_favail / f_files`를 사용한다.
- 표준 출력에는 역할별 JSON 한 줄이 나온다. `event=host_storage_health`, `schemaVersion=1`, UTC `timestamp`, 고정 `role`(`release`, `docker_data`), `status`, `reason`, 임계값과 측정값이 독립 필드다. 경로, 예외 원문, 파일 내용은 출력하지 않는다.
- 정상 측정에는 `totalBytes`, `freeBytes`, `freePercent`, `totalInodes`, `freeInodes`, `freeInodePercent`, `violations`가 포함된다. 측정 실패에는 수치가 없고 `status=error`다. 없는 값을 0 또는 정상으로 해석하지 않는다.
- `reason`: 정상 `within_thresholds`, 용량 부족 `threshold_breached`, 경로 없음 `path_missing`, 권한 부족 `path_inaccessible`, 기타 OS 실패 `stat_failed`, 미지원/잘못된 카운터 `counters_unavailable`. inode 총량 0도 확인 불가로 실패한다.
- 종료 코드: **0** 두 역할 정상, **1** 하나 이상 부족/확인 실패, **2** 잘못된 CLI 인자. 첫 역할이 실패해도 두 번째 역할을 검사한다.

공간 부족 시 release, Docker 볼륨/로그 및 백업 용량 증가 원인을 확인한다. 기록이나 볼륨을 자동 삭제하지 않는다. 이 검사는 쓰기 성공 여부, quota, 읽기 전용 마운트, I/O 오류, 개별 컨테이너 사용량 또는 백업 복구 가능성을 보장하지 않는다.

## 지속 모니터링 활성화 체크리스트

[호스트 자동화 운영 절차](host-job-automation.md)에 5분 타이머, 원자적 상태 파일, Alloy textfile 수집과 Prometheus의 실패·실행 누락 경보를 준비했다. 별도 운영 승인을 받은 뒤 실제 data-root와 실행 권한을 확인하고 설치한다. 두 역할의 `low`/`error`, 수집 누락과 마지막 성공 시각을 확인하고 승인된 알림 수신 경로로 장애·복구 알림이 도착하는지 검증해야 한다. **종료 코드만 기록하거나 타이머만 설치하는 것은 알림 연결 완료가 아니다.**

## 로컬 검증

```sh
python3 infra/scripts/test-storage-health.py
```

statvfs fixture를 사용하므로 실제 디스크 상태, Docker 또는 네트워크와 무관하다. 개별 임계값, 경계/고갈, 경로/권한 실패, 카운터 오류, 입력 검증, 고정 역할, 민감 경로 미출력과 종료 코드를 검사한다.
