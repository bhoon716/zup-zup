# 모노레포 마이그레이션 결정 문서 (Monorepo Migration Decision)

## 배경 및 맥락 (Context)
- 기존 워크스페이스는 `server/`, `web/`, `infra/` 3개의 별도 저장소로 파편화되어 운영되었습니다.
- 교차 변경 작업 시 CI/CD 배포 및 코드 소유권 경계 유지가 복잡해지는 문제가 있었습니다.
- 현행 운영 모델은 웹(`apps/web`)을 Vercel에 배포하고, 서버(`apps/server`) 및 인프라(`infra`)를 OCI 호스트에서 실행합니다.

## 결정 사항 (Decision)
- 프로젝트를 모노레포(Monorepo) 스타일의 루트 저장소 구조로 통합합니다.
- active 소스 레이아웃으로 `apps/web`, `apps/server`, `infra` 구조를 채택합니다.
- 프론트엔드(`apps/web`)는 Vercel 무중단 배포를 유지합니다.
- 백엔드 및 인프라(`apps/server`, `infra`)는 OCI ARM64 호스트 구성을 유지합니다.
- 경로 기반(Path-based) CI 파이프라인을 적용하여 각 애플리케이션의 변경사항을 독립적으로 검증합니다.

## 결과 및 시사점 (Consequences)
- 실제 공유 데이터 규격 계약이 필요한 경우에만 공통 패키지를 추가하며, 불필요한 빈 플레이스홀더 패키지는 남기지 않습니다.
- 프론트엔드 변경이 OCI 인프라 운영에 영향을 주지 않으므로 저비용 Vercel 배포 이점을 유지합니다.
- 서버 및 인프라가 동일한 호스트/런타임 모델을 공유하여 배포 피로도를 줄입니다.

## 배포 연동 규격 (Deployment Alignment)
- 기존 Vercel 프로젝트 루트 디렉터리를 `apps/web`으로 지정합니다.
- 서버 배포 명령어 실행 기준 위치를 `apps/server`로 지정합니다.
- 인프라 검증 및 Docker Compose 작업 기준 위치를 `infra`로 지정합니다.
- 루트 GitHub Actions 워크플로를 통해 경로별 변경을 검증하고 아티팩트를 OCI 호스트로 배포합니다.

## 순차 배포 안내 (Rollout Notes)
- 마이그레이션 적용 순서: `web` → `server` → `infra`.
- 모노레포 경로 정비 완료 후 기존 Vercel 프로젝트에 원샷 컷오버를 진행합니다.
- 인증, 로그인 또는 주요 페이지 반복 에러 발생 시 즉시 이전 버전으로 롤백 기준을 유지합니다.
