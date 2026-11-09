# 호스트 점검·백업 자동 실행

**현재는 코드와 오프라인 검증만 준비했다. 이 문서의 설치·시작 명령은 실행하지 않았다.** 운영 접근, 기존 작업과의 관계, 경보 수신 경로를 확인한 뒤 승인된 운영자가 활성화한다. CD는 파일만 배치하며 systemd 설치·enable·기존 작업 삭제를 수행하지 않는다.

## 실행 경로와 범위

- `sugang-helper-storage.timer`: UTC 매 5분, 최대 15초 분산. release 및 실제 Docker data-root의 여유 용량/inode를 검사한다. 변경·삭제는 하지 않는다.
- `sugang-helper-backup.timer`: 매일 UTC 19:00, 최대 5분 분산(한국 다음 날 04:00). 기존 DB/exporter가 전체 DB, 최소 감사 스냅샷과 업로드를 만든다. `BACKUP_INCLUDE_UPLOADS=0`을 명시한 경우 DB·감사만 내보낸다. Mac 전송·복구 성공을 의미하지 않는다.
- `sugang-helper-event-backup.timer`: UTC 매 5분, 최대 15초 분산. 최근 10분의 현재 app 컨테이너 로그에서 [허용된 중요 사건의 최소 필드](important-event-backup.md)만 압축·체크섬과 함께 보관한다. 원본 전체 로그나 완전한 감사 원장이 아니다.

타이머는 `Persistent=true`이므로 재시작 후 놓친 실행을 한 번 보충할 수 있다. 중단 기간 전체를 백필하지 않는다. 중요 로그의 10분 창보다 긴 중단/로그 순환/컨테이너 교체는 누락을 남길 수 있다. 두 스냅샷의 창은 겹치므로 정확한 중복 제거·발생 횟수를 보장하지 않는다.

`run-host-job.py`는 두 백업 전에 저장 공간을 다시 검사하고 부족/측정 실패이면 쓰기를 시작하지 않는다. 기본 5 GiB·15% 여유 블록·10% 가용 inode 기준은 [저장 공간 점검](host-storage-health.md)을 따른다. inode를 제공하지 않는 파일시스템도 정상으로 가정하지 않는다. 백업 중 증가를 예약하거나 예측하는 기능은 아니다.

## 실패·실행 누락 감시

고정 `task`(`storage`, `backup`, `event_backup`)마다 flock으로 중복 실행을 막고 상태 파일을 원자 교체한다. 시작 시 성공 0·실행 중 1을 게시하고, 정상 완료 후에만 마지막 성공 시각을 갱신한다. 실패는 이전 성공 시각을 보존한다. 중복 요청은 실행 중 작업의 상태를 덮어쓰지 않는다. DB 작업은 900초, 중요 이벤트는 240초 제한이며 시간 초과 시 자식 프로세스 그룹을 종료한다. systemd도 각각 16분/270초 및 storage 60초 제한으로 남은 자식까지 관리한다.

상태 경로 `/var/lib/sugang-helper-host-jobs`에는 작업별 `*.prom`과 빈 잠금 파일만 있다. 임시 파일은 `.tmp`이며 완료 전 exporter가 읽지 않는다. 상태 디렉터리는 서비스가 생성한다. Alloy에는 이 디렉터리만 읽기 전용으로 연결하며 `.prom`만 수집한다. 경로/비밀번호/사용자 데이터는 지표에 넣지 않는다. Alloy의 기존 root 실행 계정으로 읽으며 호스트 `/proc`, `/sys`, `/`를 새로 연결하지 않는다.

지표는 `sugang_host_job_success`, `running`, `last_started_timestamp_seconds`, `last_completed_timestamp_seconds`, `last_success_timestamp_seconds`, `duration_seconds`다(앞의 `sugang_host_job_` 접두어 공통). 저장 공간에는 고정 `role`별 `sugang_host_storage_healthy`, `measurement_success`, `free_bytes`, `free_percent`, `free_inode_percent`가 있다. 측정 실패 시 수치는 제거하고 성공 0을 게시한다. 누락값을 정상 0으로 대체하지 않는다.

기존 Alloy의 `prometheus.exporter.unix "host_jobs"`가 textfile 수집기만 사용한다. Prometheus `host-jobs` job은 내부 `alloy:12345`의 `/api/v0/component/prometheus.exporter.unix.host_jobs/metrics`를 수집한다. Alloy `/metrics`의 자체 지표와 다른 경로다. 새 공인 포트나 새 외부 목적지는 없다. 설정 근거: [공식 textfile 컴포넌트 문서](https://grafana.com/docs/alloy/latest/reference/components/prometheus/prometheus.exporter.unix/), [공식 저장소의 component endpoint 재현](https://github.com/grafana/alloy/issues/4330). 고정 이미지에서 실제 경로를 확인하는 CI fixture를 추가했지만 이번 로컬 작업에서 Docker 실행 검증은 하지 못했다.

`host-alerts.yml`은 지표 target 누락/실패, textfile 오류, 역할별 저장 공간 부족/누락, 작업 실패, 실행 시간 초과를 2분 지속 시 경보로 만든다. storage와 event_backup은 성공 이후 15분, backup은 26시간이 지나면 신선도 경보가 된다. 한 번도 성공하지 않은 0과 지표 누락, 미래로 60초 이상 치우친 시각도 실패로 취급한다. 현재시각을 Prometheus에서 비교하므로 파일 값이 멈춰도 감지한다. 같은 호스트의 전체 장애는 외부 감시가 필요하다.

**경보 판정은 외부 수신 완료가 아니다.** [Prometheus 알림 경로](../../infra/prometheus/README.md)를 별도로 승인·활성화하고 실제 장애/복구 수신을 확인한다. Grafana는 UI에서 관리하며 파일 대시보드를 추가하지 않는다. UI에서 `job="host-jobs"`, task/role, 마지막 성공의 경과시간 및 활성 경보를 확인한다.

## 운영 활성화 체크리스트

1. Python 3.9 이상(POSIX subprocess UID/GID 지원), systemd, Docker CLI가 있는 Linux 호스트인지 확인한다. 실제 Docker data-root, app 이름, 업로드 경로, 백업 용량을 확인한다. 자동화는 기본 production 컨테이너 이름을 사용한다. 다른 이름이면 검토한 실행 인자로 변경한다.
2. 기존 `backup-db-local.sh` 타이머/cron과 Mac 동기화 시각을 먼저 확인한다. 새 daily export를 기존 주간 작업과 병행할지 교체할지 결정한다. 이 코드가 기존 스케줄을 중지하거나 변경하지 않는다. 중복 full export와 최소 감사·이벤트 사본의 누적 비용을 검토한다.
3. 기존 release `.env`가 systemd EnvironmentFile 문법과 호환되고 DB 자격증명 및 업로드 선택이 올바른지 보안 절차로 확인한다. DB 비밀을 새 파일에 복사하지 않는다. backup 서비스만 기존 파일을 읽고, child에는 필요한 DB 비밀만 전달한다. storage/event 서비스는 해당 비밀 파일을 읽지 않는다. exporter는 기존 Docker 소켓 권한을 사용한다. Docker 접근은 호스트 수준 권한이며 이 구성으로 격리되는 것이 아니다.
4. 서비스의 runner는 root로 저장 공간 stat와 root 소유 상태 파일만 관리한다. 두 exporter 자식은 고정 `HOST_EXPORT_USER=ubuntu`의 기존 UID·기본 GID·보조 그룹으로 권한을 낮춰 실행한다. Python POSIX `Popen(user=…, group=…, extra_groups=…)`를 사용하며 preexec_fn, sudo 재실행, root fallback은 없다. 계정·그룹을 찾지 못하거나 root UID/GID/그룹으로 해석되거나 권한 변경이 실패하면 백업 작업은 실패한다. non-root로 runner를 직접 실행하면 현재 identity를 유지하며 계정 전환을 하지 않는다.

   새 산출물의 0700 디렉터리와 0600 파일은 ubuntu 소유가 되어 기본 Mac off-host sync의 `SERVER_USER=ubuntu`와 일치한다. 기존 배포는 ubuntu가 sudo 없이 Docker를 쓰는 계약이지만 **실제 Docker 그룹·소켓 접근은 확인하지 않았다.** 활성화 전에 기존 ubuntu의 Docker 실행 및 backups 경로 읽기·쓰기 권한을 확인한다. 그룹 가입·계정 생성·소켓/디렉터리 권한 변경을 자동 수행하지 않는다. 업로드는 승인된 source 읽기 권한이 필요하며 실패 시 root로 재시도하지 않는다.

   기존 backups/export/audit/events 경로나 과거 스냅샷이 root-private라면 권한을 임의로 넓히거나 재귀 chown하지 않는다. **이는 별도 승인된 소유권·보관 데이터 이관이 필요한 활성화 차단 사항**이다. 운영자가 기존 데이터의 소유자·범위를 확인하고 이관·체크섬·ubuntu 읽기 검증을 마친 후 활성화한다. 실행 스크립트와 기존 credential 파일의 변경 권한도 검토한다. `NoNewPrivileges`, 읽기 전용 시스템·홈, private tmp/network를 사용하지만 Docker API 권한 자체는 제한하지 않는다.
5. 아래 준비/설치 명령을 검토한 뒤 승인된 운영 절차로 실행한다. **ReadWritePaths 대상 backups 디렉터리는 서비스 시작 전에 존재해야 한다.** 기존 디렉터리의 소유권/권한을 임의로 바꾸거나 원본을 삭제하지 않는다.

```sh
# 승인 후 Linux 호스트에서만 실행. 예시는 기본 release 경로를 가정한다.
release=/home/ubuntu/jbnu-sugang-helper
# 없을 때만 디렉터리 생성. 기존 디렉터리/파일 권한은 먼저 검토한다.
sudo test -d "$release/backups" || sudo install -d -o ubuntu -g ubuntu -m 0700 "$release/backups"
sudo install -d -m 0755 /etc/sugang-helper
sudo install -m 0644 "$release/systemd/host-jobs.env.example" /etc/sugang-helper/host-jobs.env
sudo install -m 0644 "$release/systemd/"*.service "$release/systemd/"*.timer /etc/systemd/system/
sudo systemd-analyze verify /etc/systemd/system/sugang-helper-{storage,backup,event-backup}.{service,timer}
sudo systemctl daemon-reload
```

설정 파일을 기존 운영 값 위에 덮어쓰지 않는다. 최초 설치 이후에는 차이를 검토해 반영한다. release 경로를 바꾸면 EnvironmentFile과 ReadWritePaths도 일치시킨다. Docker data-root는 비밀이 없는 `/etc/sugang-helper/host-jobs.env`로 지정한다. 임계값은 운영자 소유 systemd drop-in으로 ExecStart를 재지정한다. 타이머 간격을 바꿀 때 Prometheus 신선도 경보도 함께 조정한다.

```sh
# 승인된 수동 첫 실행에서 실제 파일·지표·경보를 먼저 검증한다.
sudo systemctl start sugang-helper-storage.service
sudo systemctl start sugang-helper-backup.service
sudo systemctl start sugang-helper-event-backup.service
sudo systemctl status sugang-helper-{storage,backup,event-backup}.service
sudo journalctl -u sugang-helper-storage.service -u sugang-helper-backup.service \
  -u sugang-helper-event-backup.service --since '30 minutes ago' --no-pager
# 체크섬·manifest·업로드 범위·오프호스트 복사·복구 및 알림 수신 검증 후 활성화한다.
sudo systemctl enable --now sugang-helper-{storage,backup,event-backup}.timer
systemctl list-timers 'sugang-helper-*'
```

수동 실패 시 원문 비밀번호/에러를 로그에 출력하지 않는다. JSON의 고정 reason과 실패한 task, exporter의 필수 경로/권한/자격증명/컨테이너 상태를 승인된 운영 절차로 점검한다. `storage_unhealthy`, `credential_missing`, `export_failed`, `export_timed_out`, `export_unavailable`, `export_identity_unavailable`, `state_unavailable`, `already_running`을 구분한다. 자동 재시도·자동 삭제는 없으며 다음 타이머가 다시 실행한다. state가 쓰기 불가하면 실패 요약을 journal에 남기고 기존 지표의 신선도 경보가 감지한다.

타이머만 중지하려면 승인된 운영자가 `systemctl disable --now sugang-helper-<작업>.timer`를 실행한다. 이는 이미 실행 중인 service를 종료하지 않는다. 해당 서비스 중지도 필요하면 진행 중인 백업 상태를 검토한 뒤 별도로 수행한다. 기존 아카이브/DB/업로드/지표를 자동 삭제하지 않는다. 타이머를 중지하면 신선도 경보는 계속 발생하므로 중지 사유와 기간을 알림 운영 절차에 기록한다.

## 보존과 검증 한계

runner는 하위 프로세스의 stdout/stderr를 파일이나 메모리에 누적하지 않으며, 고정 JSON 상태 요약만 journald로 보낸다. 원문 로그의 새 무제한 복제는 없다. journald의 기존 용량/기간 제한을 운영에서 확인하고, Loki/컨테이너의 기존 유한 보존을 유지한다. 최소 감사/이벤트 아카이브와 서버 export는 별도 보존 대상이며 자동 삭제하지 않아 계속 용량이 늘어난다. 보존 비용·암호화·외부 사본 정책은 별도 결정해야 한다.

```sh
python3 infra/scripts/test-host-jobs.py
python3 infra/scripts/test-important-event-backup.py
python3 infra/scripts/test-storage-health.py
# Docker가 허용된 CI/검증 환경에서만:
bash infra/scripts/test-host-metrics-pipeline.sh
```

오프라인 테스트는 임시 경로, statvfs fixture, mocked child/stub Docker만 사용한다. 실제 운영 디스크, DB 백업/복구, Docker 컴포넌트 엔드포인트, ubuntu 계정·그룹과 POSIX 권한 전환, systemd sandbox와 환경 파일 처리, 외부 알림 수신은 아직 실행 검증하지 않았다. 특히 `.prom` 성공은 exporter 성공일 뿐 복원 가능성이나 누락 없는 로그 수집을 보장하지 않는다.
