# DB·첨부 파일 복구 준비 검증

이 문서는 복구 **연습**을 위한 절차다. 운영 DB를 덮어쓰거나 앱을 실행하지 않는다. 실제 서버 백업 생성·수동 다운로드·Docker 복구를 실행했다는 증거와 로컬 계약 테스트 결과를 구분한다.

## 완성된 백업 묶음

운영자가 기존 백업 환경에서 `DB_ROOT_PASSWORD`를 제공한 뒤 실행한다. 비밀번호를 명령행에 직접 쓰거나 새 파일에 저장하지 않는다.

```sh
bash infra/scripts/backup-server-export.sh
```

- 기본 `BACKUP_INCLUDE_UPLOADS=1`은 DB와 문의 첨부 파일을 모두 요구한다. 첨부 파일은 `docker exec sugang-helper-app tar -czf - -C /app/data/uploads .`로 기존 앱 mount에서 읽고, 결과를 백업 소유자의 staging 파일에 저장한다. 백업 사용자가 root 전용 `/var/lib/docker` 디렉터리를 직접 읽을 필요가 없다. 앱 컨테이너가 중지됐거나 해당 mount를 읽을 수 없으면 실패한다. `UPLOADS_VOL_PATH`를 명시한 경우에만 운영자가 확인하고 접근 가능한 host 디렉터리를 대신 사용한다. Docker data-root 경로를 추측하거나 권한을 확장하지 않는다.
- 첨부 디렉터리가 없거나 dump·압축·tar·감사 아카이브 생성이 실패하면 완성 묶음을 공개하지 않는다. `.pending.*` 작업 디렉터리를 정리하고 기존 완성 묶음은 보존한다.
- 의도적으로 DB만 보관할 때만 `BACKUP_INCLUDE_UPLOADS=0`을 지정한다. manifest에 `uploads=excluded`를 기록하며 전체 복구 준비로 간주하지 않는다.
- 성공한 묶음은 `${RELEASE_ROOT}/backups/export/full_backup_<UTC>_<random>/`에 디렉터리 단위로 공개된다. DB `.sql.gz`, 첨부 `.tar.gz`, `backup.manifest`와 각 파일의 `.sha256`을 함께 보관한다. 감사 아카이브는 별도 `backups/audit/`에 남는다.
- manifest의 `schema=full-backup-v1`, `database=sugang_helper`, `dump_file`, `uploads`, `uploads_file`은 묶음 구성만 설명한다. 원본 DB의 같은 snapshot에 대한 정확한 행 수를 별도로 보증하지 않는다.
- `--single-transaction`은 MySQL transactional table의 일관된 dump를 위한 옵션이다. 그 뒤 생성하는 첨부 tar와 DB 사이의 동일 시점 원자성, 동시에 실행한 DDL의 안전성을 보증하지 않는다. 운영 복구 시 첨부 연결을 별도로 확인해야 한다.
- 수동 `backup-to-macbook.sh` 전송 정책은 유지한다. 이 변경이 외부 자동 전송이나 운영 설치를 수행하는 것은 아니다. 새 exporter에는 기존 백업을 삭제하는 pruning이 없다. 따라서 일일 timer로 활성화하면 완성 묶음이 누적되고, ADR의 서버 최신 세 개 정책이 자동 적용되지 않는다. 활성화 전에 수동 offhost 사본 확보와 보관·회전 정책을 확정해야 한다. 이번 변경은 기존 기록·백업을 삭제하지 않는다. 저장 공간 preflight/신선도 경보가 있어도 장기 여유 공간을 보장하지 않는다.

## Docker 실행 전 준비

1. 선택한 백업을 다룰 권한, 로컬 디스크의 접근 제한·여유 공간, 백업 출처와 시각을 확인한다. SQL·첨부 원문을 공유 채널에 출력하지 않는다.
2. 저장소와 백업의 릴리스 버전을 맞춘다. 현재 전체 DB 검증은 V32의 37개 테이블 목록과 checkout의 마지막 Flyway 버전을 기준으로 한다. 오래된 백업을 통과시키려고 자동 migration하지 않는다.
3. Python 3.8+, Bash, gzip, tar, shasum, Docker와 **이미 로컬에 준비된** 공식 MySQL 8.4 이미지가 필요하다. 스크립트는 `--pull=never`라서 이미지를 자동 다운로드하지 않는다.
4. 운영자가 사전에 검증한 공식 이미지 digest를 사용하는 것을 권한다. 로컬 이미지 정보를 확인하는 명령은 다음과 같다. 등록소 조회나 pull은 이 검증 스크립트의 일부가 아니다.

```sh
docker image inspect mysql:8.4 --format '{{json .RepoDigests}}'
```

`RESTORE_MYSQL_IMAGE`은 `mysql:8.4`, `mysql:8.4.<patch>` 또는 `mysql@sha256:<64자리 digest>`만 허용한다. digest가 8.4인지도 컨테이너의 `SELECT VERSION()`으로 확인한다. 기본 tag는 변할 수 있으므로 실행 결과와 실제 digest를 운영자가 함께 기록한다. Docker 연결/권한 오류가 나면 해당 접근 문제를 해결하기 전 다른 socket이나 context로 우회하지 않는다.

## 격리 복구 연습

사용할 묶음을 명시하는 방식을 권한다. 다음 명령의 경로와 digest는 실제로 확인한 값으로 바꾼다.

```sh
RESTORE_MYSQL_IMAGE='mysql@sha256:<검증한-digest>' \
  bash infra/scripts/verify-mac-restore.sh \
  "$HOME/Backups/jbnu-sugang-helper/mysql/export/<full_backup_묶음>/<DB파일>.sql.gz"
```

경로를 생략하면 `${LOCAL_BACKUP_DIR:-$HOME/Backups/jbnu-sugang-helper}/mysql`에서 필요한 구성 파일과 checksum이 있는 후보 중 dump 수정 시각이 가장 최근인 것을 고른다. 전송은 원래 수정 시각을 보존한다. 숨김 staging·부분 전송·symlink·감사 전용 dump는 자동 후보에서 제외한다. 새 형식은 manifest와 첨부 포함 시 tar/checksum도 요구한다. 선택한 최신 후보의 체크섬이 틀리면 이전 백업으로 조용히 넘어가지 않고 실패한다. 수정 시각은 snapshot 신뢰성이나 백업 최신성 SLA 자체를 증명하지 않으므로 운영 기록과 대조한다.

감사 전용 연습은 별도 파일을 지정한다.

```sh
bash infra/scripts/verify-mac-restore.sh \
  "$HOME/Backups/jbnu-sugang-helper/audit/<snapshot>/<archive>.sql.gz"
```

### 실제로 확인하는 것

- Docker 실행 전에 DB checksum과 gzip 무결성을 확인한다. 새 전체 묶음은 manifest checksum·파일명·범위와 첨부 tar checksum·gzip·tar 읽기까지 확인한다. 첨부를 추출하지 않는다. 이전 DB 단독 파일과 명시적 DB-only 묶음은 제한을 JSON 이벤트로 남긴다.
- 먼저 Docker의 실제 선택 endpoint가 local `unix://` socket인지 확인한다. `DOCKER_CONTEXT` 우선순위와 `DOCKER_HOST`를 반영하고 원격 SSH/TCP context는 거부한다. 다른 context로 자동 전환하지 않는다. 이어 독립 MySQL 컨테이너를 `--network none`으로 만든다. 호스트 포트, host bind mount, 운영 volume, 앱/worker 연결은 없다. MySQL event scheduler와 local infile도 기본 비활성화한다. 실제 운영 계정 비밀번호를 사용하지 않는다.
- 컨테이너 생성 후 얻은 고유 ID만 start/exec/삭제 대상으로 삼는다. 생성 실패 때 같은 이름의 기존 컨테이너를 지우지 않는다.
- TCP `SELECT 1`로 영구 MySQL 프로세스 준비를 확인한다. 이미지 초기화 중 socket-only 임시 MySQL이 떠 있다는 이유로 import를 시작하지 않는다. `RESTORE_WAIT_ATTEMPTS` 기본 120회, 허용 1~600회이며 MySQL 연결 timeout은 회당 2초, 실패 후 대기는 1초다. Docker daemon 자체 응답 지연까지 포함한 총 wall-clock timeout은 아니다.
- 전체 DB는 37개 base table 목록, 실패 Flyway 이력 없음, checkout의 마지막 migration 버전 일치, 모든 테이블 `count(*)` 실행 성공과 숫자 결과를 확인한다. `users`나 `courses`가 적법하게 0행인 백업도 허용한다. 누락 테이블이나 query 실패를 0으로 바꾸지 않는다.
- 감사 DB는 archive table 이름, 컬럼 이름·타입·nullable 여부, id primary key와 manifest의 정확한 행 수를 확인한다. 0행도 정확히 비교한다.
- 원문 SQL/error/사용자 행은 출력하지 않고, 의미 있는 단계와 테이블별 개수만 독립 JSON 필드로 기록한다. 성공 이벤트는 해당 임시 컨테이너와 익명 volume 제거가 완료된 뒤에만 나온다. 정리에 실패하면 nonzero exit와 대상 ID가 남는다.

`SIGINT`/`SIGTERM`에는 정리를 시도하지만 강제 종료나 Docker 장애 때는 임시 자원이 남을 수 있다. 실패 이벤트의 정확한 임시 ID를 확인해서 그 컨테이너만 정리한다. `docker compose down -v`, 전체 volume prune 같은 명령으로 처리하지 않는다.

### 성공 결과의 한계

전체 DB의 count는 테이블 읽기 가능성을 보일 뿐, 원본 snapshot과 행 수가 같거나 데이터가 빠짐없이 보관됐다는 증명이 아니다. 모든 컬럼·인덱스·FK의 Flyway validate, 애플리케이션 시작/기능 동작, 알림 재발송 방지, 첨부 참조 일치와 파일 재추출, 운영 교체는 별도 검증 대상이다. checksum은 전송 손상 검출용이며 서명·암호화·변조 불가능 보관이 아니다.

운영 교체 전에는 복구 시점과 이후 유실 범위, 앱·worker 정지, 복구된 outbox/delivery의 중복 전송 처리, 선택 릴리스의 migration 검증과 첨부 복원을 운영자가 별도로 결정한다. 이 스크립트가 통과했다고 앱을 자동 시작하지 않는다.

## 로컬 계약 테스트와 남은 확인

```sh
bash infra/scripts/test-backup-server-export.sh
bash infra/scripts/test-admin-audit-archive.sh
bash infra/scripts/test-backup-to-macbook.sh
bash infra/scripts/test-verify-mac-restore.sh
```

테스트는 합성 파일과 Docker 명령 stub을 사용한다. 현재 migration 파일의 CREATE/DROP 테이블 목록을 fixture 목록과 대조하지만 SQL migration이나 실제 MySQL engine을 실행하는 통합 테스트는 아니다. 실제 운영 DB 복구 성공으로 표현하지 않는다.

2026-11-09 후속 확인에서 11월 5일 생성된 로컬 DB dump 사본(10,745,453 bytes)과 checksum을 별도 보관 위치에서 찾았다. SHA-256 대조와 `gzip -t`는 통과했다. 이 사본은 DB만 확인된 입력이며, 업로드 묶음·실제 MySQL 복원·애플리케이션 정상 동작을 검증한 것은 아니다. Docker socket의 기존 접근 제한을 재시도하거나 우회하지 않아 실제 복원 연습은 미완료다. 허용된 격리 환경에서 백업 시각·이미지 digest·결과·정리 완료를 기록해야 한다. 운영 서버 접근·배포·스케줄 등록은 이 문서 작성으로 수행되지 않았다.
