# [P1][infra] 로컬 앱 이미지 build·pull 경로 분리

## 문제

다음 명령으로 인프라를 시작할 때 앱 서비스가 로컬 이미지에 없었다.

```text
cd infra
docker compose up -d
Error response from daemon: pull access denied for sugang-helper, repository does not exist or may require 'docker login'
```

현재 `app` 서비스는 `image`만 선언되어 있어 로컬에서는 존재하지 않는 이미지가 레지스트리에서 pull되고, 운영에서는 어떤 이미지 저장소와 태그를 사용해야 하는지와 섞여 있다.

## 완료 기준

- 로컬에서 앱 이미지를 build하고 Compose를 올리는 표준 명령을 제공한다.
- 운영에서는 사전에 빌드·게시된 이미지를 명시적인 이름과 태그로 pull한다.
- 이미지가 없거나 이름이 잘못된 경우 pull access denied 대신 원인과 해결 방법을 안내한다.
- Compose 정책 또는 smoke가 로컬·운영 이미지 경로의 혼동을 검출한다.

## 관련 파일

- `infra/docker-compose.yml`
- `infra/.env.example`
- `infra/README.md`
- `apps/server/Dockerfile`
- 배포 workflow의 이미지 build/publish 단계
