# 중요 기록 보존과 복구

일반 진단 로그를 무제한 저장하지 않고, 중요한 운영 사실을 별도 보존한다. 이 변경은 로컬 구현이며 운영 배포·스케줄 등록·오프호스트 전송 실행 여부를 뜻하지 않는다.

## 보존 범위

- `admin_action_logs`: `APP_ADMIN_AUDIT_RETENTION_DAYS=0`이 기본이며 자동 삭제하지 않는다. 기존 환경에 180 등의 값이 있으면 명시 값이 우선한다. 1~36500일은 **삭제를 허용하는 명시적 선택**이며 아카이브 성공을 확인하는 자동 게이트는 없다. 영구 보존 요구에는 0을 유지한다. 음수/범위 초과는 시작 시 거부한다.
- 감사 아카이브: ID, 관리자 ID, 작업 종류, 대상 종류/ID, 생성/변경 시각만 보존한다. `meta_data`, 답글 본문, 첨부파일, 사용자 테이블, 토큰, 오류 원문은 제외한다. 따라서 상태 변경의 전후 값 등 상세 메타데이터 복원용이 아니다. 기존 DB의 감사 JSON에는 최소화된 지문/상태가 포함될 수 있고, 오래된 데이터의 내용은 별도로 점검해야 한다.
- `crawler_run_failures`: DB에 시각·단계·종류·최대 500자 오류 설명이 저장되고 전체 DB 백업에 포함된다. 감사 전용 아카이브에는 포함하지 않는다. 진단 문자열의 장기 보존/삭제 정책은 별도 검토 대상이다. DB 장애로 저장이 실패한 사건은 파일 로그·알림만 남을 수 있다.
- 배포: 현재 배포 스크립트의 SHA/단계 출력과 CI 실행 기록이 근거다. 감사 아카이브에 포함하지 않으며 영구 보존을 보장하지 않는다.
- 전체 DB·업로드 백업은 복구용이며 Mac 기본 30일 회전 정책을 유지한다. 애플리케이션 로그/Loki/컨테이너 로그도 각 유한 보존 정책을 유지한다. 원문 로그를 영구 보관하지 않는다.

## 내보내기와 보관

기존 서버 백업 작업이 사용하는 `DB_ROOT_PASSWORD`를 환경으로 제공한다. 비밀번호를 CLI 인자에 직접 작성하거나 파일에 새로 저장하지 않는다. 기본 비밀번호 fallback은 제거했다.

```sh
bash infra/scripts/backup-server-export.sh
# 또는 감사 기록만 내보내기
bash infra/scripts/backup-admin-audit.sh
```

서버의 `${RELEASE_ROOT}/backups/audit/admin_audit_<UTC>_<random>/`에 생성한다. 읽기 전용 SELECT 하나에서 행을 가져와 별도 `admin_action_logs_archive` SQL 테이블로 보관한다. 원본 DB와 스키마는 수정하지 않는다. 실패하면 `.pending.*`만 정리하고 완성된 아카이브를 공개하지 않는다. 파일은 소유자 전용 umask로 만들며 압축 파일, schema/행 수 manifest, 각각 SHA-256 파일을 포함한다.

기존 `backup-to-macbook.sh`는 이 경로를 함께 전송하고 체크섬을 확인한 다음 `${LOCAL_BACKUP_DIR}/audit/`에 보관한다. 감사 경로는 `mysql/`의 회전 삭제 대상 밖에 있고 같은 이름의 다른 내용으로 덮어쓰지 않는다. 체크섬/manifest 누락·불일치 시 승격과 회전 삭제를 중단한다. 일반 백업의 `RETENTION_DAYS`는 1~9999일만 허용한다.

현재는 실행할 때마다 전체 최소 감사 스냅샷을 남긴다. 중복 저장과 디스크 증가를 고려해 기존 백업 주기를 결정하고 저장 공간을 감시한다. 자동 삭제/증분 수집/외부 저장소/IAM을 추가하지 않는다. SHA-256은 전송 손상 검출이며 악의적 관리자의 변조 방지 서명이나 WORM 보장은 아니다. 파일은 암호화되지 않으므로 접근 제한된 디스크에서만 보관하고 공유하지 않는다.

## 자동 실행과 추가 사본

[호스트 자동 실행 템플릿](host-job-automation.md)은 일일 DB·감사·업로드와 5분 저장 공간/최소 사건 로그 작업을 준비한다. 실제 설치·활성화는 하지 않았다. [중요 사건 로그](important-event-backup.md)는 `backups/events/`에 별도 보관하며 Mac 전송 시 `events/`에 보존한다. 감사/events는 일반 백업 회전 대상이 아니다.

새 전체 DB·업로드 묶음은 완성 디렉터리로 원자 게시·전송한다. Mac의 기존 유한 보존 정책은 모든 구성 파일이 만료된 뒤 묶음 전체에 적용해 부분 파일 삭제가 다음 동기화를 막지 않도록 한다. 서버의 새 exporter에는 자동 pruning이 없으므로 장기 비용·기존 최신 세 개 정책과의 관계를 활성화 전에 결정해야 한다. 실제 백업·기록 삭제는 이번 작업에서 실행하지 않았다.

## 복구 검증

```sh
bash infra/scripts/verify-mac-restore.sh "$HOME/Backups/jbnu-sugang-helper/audit/<snapshot>/<archive>.sql.gz"
```

복구 전 압축 파일과 manifest 체크섬을 확인한다. 임시 MySQL 8.4 컨테이너(호스트 포트/운영 볼륨 없음)에 복원한 뒤 테이블 1개와 manifest의 정확한 행 수를 검증하고 컨테이너를 제거한다. 0행 아카이브도 0행 일치를 확인한다. 준비 시간 초과, SQL 실패, 행 수 불일치는 실패한다. 실제 운영 데이터로 복구 검증을 수행하기 전 접근 권한과 로컬 보관 위치를 확인한다.

로컬 테스트:

```sh
bash infra/scripts/test-admin-audit-archive.sh
bash infra/scripts/test-backup-to-macbook.sh
bash infra/scripts/test-verify-mac-restore.sh
cd apps/server
./gradlew test --tests '*AdminAuditLogRetentionSchedulerTest' --offline
```

CD release 묶음은 `scripts/backup-server-export.sh`와 필수 sibling `scripts/backup-admin-audit.sh`를 함께 포함한다. 배포가 이 파일을 준비해도 기존 `backup-db-local.sh` timer의 호출 대상은 자동 변경하지 않는다. 단독 수동 설치 시에도 두 파일을 같은 디렉터리에 배치해야 한다.

운영 적용 후 확인할 것: 기존 환경의 감사 보존 값, 백업 작업의 새 스크립트 호출, 최근 아카이브의 시각·체크섬·행 수, Mac 전송 결과, 실제 데이터 복구 연습, 여유 디스크. 코드가 준비되었다는 사실만으로 이미 운영 기록이 영구 보관되고 있다고 판단하지 않는다.
