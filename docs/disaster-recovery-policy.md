# 데이터베이스 및 지속 데이터 복구 정책 (Disaster Recovery Policy)

> **현재 운영 정책 (2026-07-26)**: MySQL 데이터는 Docker 볼륨 `sugang-helper-db-data`에 보관되며, OCI 서버 내부 주간 백업(`backup-db-local.sh`) 및 **운영자 로컬 맥북(Local MacBook) 오프호스트 백업 파이프라인(`infra/scripts/backup-to-macbook.sh`)**을 통해 OCI 서버 전체 파손(Host-loss) 시에도 완전히 복구할 수 있도록 수립되었습니다.

---

## 1. RPO / RTO 정의 (복구 목표)

* **RPO (Recovery Point Objective, 목표 복구 시점)**: **최대 24시간**
  - OCI 서버 인스턴스가 전면 파손되더라도 최신 일일/주간 백업을 통해 최대 24시간 이내의 데이터로 복구합니다.
* **RTO (Recovery Time Objective, 목표 복구 시간)**: **30분 이내**
  - 재해 발생 시 신규 서버 또는 운영자 맥북의 Docker 환경으로 DB 및 첨부파일을 30분 이내에 정상 복구합니다.

---

## 2. 데이터베이스 접근 권한 및 보안 경계

1인 운영 체제 특성에 맞춘 최소 인프라 복잡도를 유지합니다:

| 주체 | 계정 | 용도 |
| --- | --- | --- |
| MySQL 컨테이너 | `root` | 데이터베이스 관리 및 초기화 |
| 애플리케이션 (Spring Boot) | `root` | 런타임 데이터 CRUD 조작 |
| 일회성 Flyway 서비스 | `root` | 배포 시 스키마 자동 마이그레이션 |

- OCI 서버의 `/home/ubuntu/jbnu-sugang-helper/.env` 파일에만 DB 및 Redis 비밀번호를 암호화 보관합니다.
- Git 저장소(`apps/server/.env`)에는 실제 DB 접속 비밀값을 포함하지 않습니다.

---

## 3. 백업 주기 및 수행 방식

백업은 **1차 서버 내부 백업**과 **2차 맥북 오프호스트 백업**의 이중 구조로 실행됩니다.

```text
[OCI 서버 MySQL] ──(1차 주간 덤프)──> OCI 서버 내 local backup (/backups/mysql)
                                            │
                                  (2차 맥북 오프호스트 수집)
                                            ▼
                               [운영자 맥북] (~/Backups/jbnu-sugang-helper)
```

### ① OCI 서버 내부 백업 (1차 백업)
* **백업 주기**: **매주 월요일 새벽 04:00 (Asia/Seoul)**
* **수행 방식**: OCI 서버의 `jbnu-sugang-helper-db-backup.timer` (systemd timer)로 자동 실행
* **생성 파일**: `sugang_helper_YYYYMMDD.sql.gz` + SHA-256 체크섬 사이드카 파일
* **특징**: 서비스 중단 없이 `--single-transaction` 트랜잭션 일관성 덤프 수행

### ② 맥북 오프호스트 백업 (2차 독립 백업)
* **백업 주기**: **매일 1회 (추천: 매일 새벽 05:00 또는 맥북 부팅 시)**
* **수행 스크립트**:
  - **서버 측 추출**: `infra/scripts/backup-server-export.sh` (DB 덤프 + 업로드 첨부파일 압축)
  - **맥북 수집/검증**: `infra/scripts/backup-to-macbook.sh` (SSH/rsync 전송, SHA-256 검증, 보관 정리)
* **실행 방법**:
  - **수동 실행**: 맥북 터미널에서 `./infra/scripts/backup-to-macbook.sh` 즉시 실행
  - **자동 실행**: 맥북 `crontab -e`에 다음 한 줄 추가
    ```bash
    0 5 * * * /Users/bhoon/Project/jbnu-sugang-helper/infra/scripts/backup-to-macbook.sh >> ~/Backups/jbnu-sugang-helper/logs/cron.log 2>&1
    ```
* **보관 기간 (Retention)**: **최근 30일간**의 백업본을 맥북에 유지하며, 30일이 지난 오래된 백업 파일은 자동 삭제됩니다.

---

## 4. 복원 훈련 (Clean Restore Drill)

실제 운용 중인 데이터베이스를 마이그레이션 없이 복원하는 것은 위험하므로, 백업 데이터의 정합성은 **맥북의 격리된 임시 Docker 컨테이너**에서 검증합니다.

```bash
# 맥북 터미널에서 복원 훈련 실행
./infra/scripts/verify-mac-restore.sh [백업파일경로.sql.gz]
```

**복원 훈련 검증 단계**:
1. 맥북 로컬 Docker에 임시 `mysql:8.4` 컨테이너 자동 생성
2. 백업받은 `sugang_helper_YYYYMMDD.sql.gz` 덤프 복원
3. `users`, `courses` 등 주요 테이블 생성 및 레코드 건수 정합성 검증
4. 검증 완료 후 임시 Docker 컨테이너 자동 삭제 (Clean Cleanup)

---

## 5. 복구 범위 및 한계점 (Boundary)

- **지원되는 복구 시점**: 맥북 및 서버에 정상 저장된 **가장 최근의 논리 덤프 시점**
- **PITR (Point-in-Time Recovery)**: 실시간 바이너리 로그 백업을 통한 초 단위 복구는 1인 운영 인프라 단순화를 위해 범위에서 제외되어 있습니다.
