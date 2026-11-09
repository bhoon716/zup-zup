# 최소 중요 이벤트 백업

이 문서는 **로컬에서 작성·오프라인 검증한 운영 도구**의 사용 계약이다. 도구 작성만으로 서버 실행, 타이머 설치, 배포 또는 원격 전송이 이루어지지 않는다. 관리자 감사 DB 아카이브와 별개이며 원본 로그 전체를 영구 보관하지 않는다.

## 실행

```sh
python3 infra/scripts/backup-important-events.py \
  --release-root "$RELEASE_ROOT" \
  --container sugang-helper-app \
  --timeout-seconds 210
```

- 기본 조회 창: 실행 시각 기준 이전 10분. 호스트 자동화의 `event_backup` 작업은 5분 주기, 외부 제한 240초로 호출하도록 설계했다.
- `--since`, `--until`은 시간대가 명시된 ISO8601 시각이다. 조회 구간은 양수이며 최대 1시간이다. 종료 시각 생략 시 현재 UTC, 시작 시각 생략 시 종료 10분 전을 사용한다.
- `--container`는 영문자·숫자로 시작하는 최대 128자 Docker 이름만 허용한다. 셸을 거치지 않고 고정된 `docker logs --since … --until … <container>` 인자 배열로 실행한다. 전체 컨테이너 검색, `exec`, DB 접근은 하지 않는다.
- Docker CLI 및 로그 읽기 권한이 필요하다. 별도 Python 패키지는 필요하지 않다. Docker 권한을 새로 부여하거나 인증 설정을 변경하지 않는다.
- 최대 입력 64 MiB, 한 줄 256 KiB, Docker 조회/파싱 210초. 제한 초과, Docker 비정상 종료, 파일 쓰기 오류는 실패다. 잘린 입력으로 성공 아카이브를 게시하지 않는다.

## 저장 계약

`$RELEASE_ROOT/backups/events/events-<UTC시각>-<임의ID>/`에 다음 네 파일을 함께 게시한다.

- `events.jsonl.gz`: 허용된 최소 이벤트만 담는 gzip JSONL.
- `events.jsonl.gz.manifest`: 조회 창, 컨테이너, 허용 이벤트 목록/필터 버전, 입력 바이트, 보존 건수와 건너뛴 줄 수, 불완전한 원천 범위 표시.
- `events.jsonl.gz.sha256`, `events.jsonl.gz.manifest.sha256`: 각 대상 파일의 SHA256과 파일명을 담은 한 줄 형식. 같은 디렉터리에서 `shasum -a 256 -c events.jsonl.gz.sha256`와 `shasum -a 256 -c events.jsonl.gz.manifest.sha256`로 확인할 수 있다. SHA256은 우발적인 변조·전송 오류 확인용이며 서명/진위 보장은 아니다.

로그 원문은 메모리에서만 읽고 필터링한 JSON을 직접 gzip에 쓴다. 스냅샷 디렉터리 0700, 파일 0600으로 생성하며 동일 파일시스템의 비공개 임시 디렉터리에서 파일/디렉터리 fsync 후 rename으로 전체 게시한다. 종료 코드 0과 성공 이벤트는 게시 후에만 반환한다. 게시 직후 디렉터리 fsync가 실패하면 완전한 디렉터리가 남아도 실패로 보고한다. 일반 실패 시 이번 실행의 임시 디렉터리만 정리한다. SIGKILL/전원 차단 시 `.partial-events-*`가 남을 수 있으므로 동기화·복구 대상은 `events-*`만 선택한다. 기존 백업의 자동 삭제는 없다. 직접 실행한 프로세스의 사용자 소유로 생성하며, systemd runner는 exporter 자식을 기존 ubuntu identity로 실행해 off-host sync 사용자와 일치시킨다. 기존 root-private 경로는 자동 chown/권한 확장을 하지 않고 별도 운영 이관이 완료될 때까지 활성화를 중단한다.

## 필드와 이벤트 범위

현재 서버 `logback-spring.xml`의 logstash JSON과 `application.yml` 설정이 원천이다. logstash `@timestamp`를 `timestamp`로 저장하며, `timestamp` 별칭도 읽는다. 시각은 명시적 시간대와 최대 9자리 소수초를 검증하고 원래 정밀도를 보존한다. 원천 JSON의 중복 키는 거부한다.

필수 필드는 `timestamp`, 고정 허용 목록의 `event`, `INFO`/`WARN`/`ERROR`의 `level`뿐이다. 선택 필드는 다음과 같다.

- `stage`: `FETCH_PARSE`, `PERSIST`, `JUMP_BOOTSTRAP`, `COURSE_API` 중 하나.
- `exceptionType`, `errorType`, `rootCauseType`, `originalType`, `recordingType`, `alertType`: 최대 128자 Java 단순 타입명 문법.
- `crawlerRunId`: 소문자 UUID 문법의 서버 생성 수집 실행 ID.

메시지, 스택, URL, 요청·응답 본문, 원문 오류, 사용자/알림 수신자 ID, 임의 속성, correlation ID는 저장하지 않는다. 선택 필드의 검증 실패는 해당 필드 제외로 처리한다. 필수 필드가 잘못되거나 조회 창 밖인 이벤트, 비 JSON/과거 형식, 미허용 이벤트는 건너뛰고 개수를 기록한다.

허용 이벤트는 서버 소스에서 확인한 수집 성공/실패 및 신선도 실패, 실패 처리 자체의 실패, API/HTTP 실패, 알림 처리·백로그 조회 실패, Slack 전송 거부/실패·종료 시 포기, 관리자 감사 정리 완료다. 정확한 문자열 목록은 exporter의 `EVENTS` 및 각 manifest에 있다. 일반 요청/시작/debug 이벤트나 아직 구조화 이벤트가 아닌 로그는 보존하지 않는다. 새 중요 이벤트를 추가할 때는 소스·개인정보 노출을 검토하고 허용 목록과 테스트를 함께 갱신한다.

## 완전성, 중복, 복구 한계

- 성공은 **현재 컨테이너에서 조회 가능한 로그의 필터링·게시 성공**만 의미한다. manifest는 항상 `completeHistory: false`, `coverage: available_container_logs_only`를 기록한다. 0건 성공도 장애가 없었다는 증거가 아니다.
- Docker 로그 순환, 컨테이너 삭제/재생성, 자동화 중단이 10분을 넘는 경우, 파싱 거부, 서버/호스트 시계 차이 때문에 누락이 생길 수 있다. 과거 컨테이너나 이미 회전 삭제된 데이터를 복원하지 못한다. Docker 원본 보존기간은 변경하지 않는다.
- 5분 주기/10분 창은 고의로 겹친다. 조회 시점 경계 이벤트도 두 스냅샷에 포함될 수 있다. 분석 시 정렬된 전체 보존 JSON 필드의 동일성으로 중복을 줄일 수 있지만 고유 원천 이벤트 ID가 없어서 서로 다른 실제 이벤트까지 합칠 수 있다. 정확한 발생 횟수나 완전한 감사 증거로 사용하지 않는다.
- 조회 실패 후 다음 실행은 기본 최근 10분만 조회한다. 영속 커서, 자동 재시도 구간 확장, 누락 복구를 제공하지 않는다. 필요한 과거 구간은 로그가 남아 있을 때 운영자가 명시적으로 지정한다.
- 백업은 같은 호스트에 있으므로 호스트 손실 방지에는 별도 검증된 동기화가 필요하다. 압축을 해제해 열람할 수 있으나 운영 DB 상태를 복원하는 도구는 아니다.
- 누적 백업의 용량은 계속 늘어난다. 저장 공간 경보와 보존 정책 검토가 필요하며 이 도구는 사용자 데이터나 기존 백업을 자동 삭제하지 않는다.

## 오프라인 검증

```sh
python3 infra/scripts/test-important-event-backup.py
```

실제 Docker/네트워크/운영 데이터 없이 Python 자식 프로세스를 Docker CLI 대역으로 사용한다. 허용 필드, 숨김 정보 제외, 해시, 권한, 제한·실패·시간 초과 시 미게시, 잘못된 인자, 중복 키, 조회 창 중복 및 시각 별칭을 검증한다. 실제 운영 로그 접근, 전원 차단 복구, 원격 복사, 타이머 실행은 별도 운영 검증 사항이다.
