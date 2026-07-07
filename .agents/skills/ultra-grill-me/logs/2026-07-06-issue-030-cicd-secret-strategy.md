# ISSUE-030 CI/CD Secret Strategy Grill Session

- 날짜: 2026-07-06
- 케이스: technical-design-grill
- 스킬 설치 루트: /Users/bhoon/Project/jbnu-sugang-helper/.agents/skills/ultra-grill-me
- 상태: 진행 중

## 요약

운영 비밀값을 `application-prod.yml`에서 제거하려고 하며, CD와 런타임 주입 방식 중 어떤 경로를 표준으로 둘지 검증 중입니다.

## 질문 로그

### Q1. 운영 비밀값은 어떤 주입 모델로 갈까요?

- 막힌 결정: 비밀값을 GitHub Actions에서 서버로 어떻게 전달할지 정하지 않으면 `application-prod.yml`, `docker-compose.yml`, `cd.yml` 수정 방식이 결정되지 않습니다.
- 제공된 옵션:
  - 1. `(추천)` GitHub Actions secrets를 서버 환경변수로 주입하고, `docker compose`가 `${VAR}` 형태로 읽게 한다
  - 2. 서버에 미리 배치한 `.env` 또는 secret file만 읽고, CD는 코드만 배포한다
  - 3. 파일 마운트형 secret만 쓰고 나머지는 env var로 혼합한다
  - 4. 다른 옵션 더 추천받기
  - 5. 직접 답변
- 답변: 1
- 결과: 확정
- 해석 결과: GitHub Actions secrets를 서버 실행 환경으로 전달하고, `docker compose`가 환경변수 치환으로 읽는 모델을 채택함.
- 새로 확정된 결정: 비밀값은 GitHub Actions에서 주입하고, repo에 비밀값을 남기지 않는 방향으로 간다.
- 남은 열린 질문: 서버가 그 환경변수를 어떤 형태로 받는지, 그리고 `application-prod.yml`을 템플릿으로 유지할지 제거할지.

### Q2. 서버는 배포된 비밀값을 어떤 형태로 받나요?

- 막힌 결정: 서버가 CI로부터 받은 비밀값을 `.env` 파일로 보관할지, 배포 프로세스의 shell environment로만 쓸지 정해야 `cd.yml`과 `docker-compose.yml` 수정 방법이 결정됩니다.
- 제공된 옵션:
  - 1. `(추천)` 배포 시 서버에 `.env` 파일을 생성하고 `docker compose`가 그 값을 읽게 한다
  - 2. SSH 스크립트의 `export`/inline env로만 전달하고 파일은 남기지 않는다
  - 3. `.env`와 inline env를 혼합한다
  - 4. 다른 옵션 더 추천받기
  - 5. 직접 답변
- 답변: 1
- 결과: 확정
- 해석 결과: 배포 시 서버에 `.env` 파일을 생성하고 `docker compose`가 이를 읽는 모델을 채택함.
- 새로 확정된 결정: CI/CD는 서버에 `.env`를 생성해 배포하는 방식으로 간다.
- 남은 열린 질문: `application-prod.yml`을 repo에 어떤 형태로 남길지 정해야 한다.

### Q3. `application-prod.yml`은 어떻게 처리할까요?

- 막힌 결정: `.env` 모델을 택해도 Spring 설정 파일 자체를 repo에 둘지, 비밀 없는 템플릿으로 둘지, 아니면 제거할지 정해야 `docker-compose.yml`과 배포 산출물이 결정됩니다.
- 제공된 옵션:
  - 1. `(추천)` 비밀 없는 `application-prod.yml` 템플릿을 repo에 남기고 `${VAR}`로 치환한다
  - 2. `application-prod.yml`을 repo에서 제거하고 모든 설정을 compose env로만 분산한다
  - 3. `application-prod.yml`은 유지하되 운영용 값은 서버 전용 `.env`로만 주입한다
  - 4. 다른 옵션 더 추천받기
  - 5. 직접 답변
- 답변: 1
- 결과: 확정
- 해석 결과: 비밀 없는 `application-prod.yml` 템플릿을 repo에 남기고, 값은 `${VAR}` 치환으로 주입한다.
- 새로 확정된 결정: `application-prod.yml`은 템플릿으로 유지하되, 기본값 없이 환경변수만 받는다.
- 남은 열린 질문: `.env`와 `application-prod.yml` 사이에 어떤 값들을 각각 두는지 정해야 한다.

### Q4. 어떤 값을 `.env`로 빼고 어떤 값을 파일에 둘까요?

- 막힌 결정: secrets뿐 아니라 호스트/URL/ID 같은 배포 환경 값까지 외부화할지 정해야 `.env` 스키마와 `docker-compose.yml` 수정 범위가 확정됩니다.
- 제공된 옵션:
  - 1. `(추천)` 비밀값만 `.env`로 빼고, host/port/URL/client-id 같은 비비밀 설정은 `application-prod.yml`에 남긴다
  - 2. 비밀값과 배포 환경값을 모두 `.env`로 빼고 `application-prod.yml`은 거의 비워둔다
  - 3. 비밀값은 `.env`, 파일 경로/고정 설정은 `application-prod.yml`에 남긴다
  - 4. 다른 옵션 더 추천받기
  - 5. 직접 답변
- 답변: 3
- 결과: 확정
- 해석 결과: 비밀값은 `.env`로, 파일 경로와 고정 설정은 `application-prod.yml`에 남기는 하이브리드 모델을 채택함.
- 새로 확정된 결정: 비밀값은 `.env`, 파일 경로/고정 설정은 `application-prod.yml`에 둔다.
- 남은 열린 질문: 어느 항목이 “고정 설정”인지 경계를 구체화해야 한다.

### Q5. 어떤 항목을 `.env`로 빼고 어떤 항목을 `application-prod.yml`에 둘까요?

- 막힌 결정: URL/redirect/cors 같은 값이 파일에 남아도 되는지, 아니면 환경별 배포값으로 `.env`에 넣을지 정해야 실제 수정 목록이 확정됩니다.
- 제공된 옵션:
  - 1. `(추천)` 비밀값만 `.env`로 두고, URL/redirect/CORS 같은 비밀 아닌 배포값은 `application-prod.yml`에 남긴다
  - 2. 비밀값 + URL/redirect/CORS는 `.env`, 나머지 고정값만 파일에 남긴다
  - 3. 비밀값 + URL/redirect/CORS + DB/Redis host까지 `.env`, 파일은 정말 고정값만 남긴다
  - 4. 다른 옵션 더 추천받기
  - 5. 직접 답변
- 답변: 3
- 결과: 확정
- 해석 결과: 비밀값 + URL/redirect/CORS + DB/Redis host까지 `.env`로 두고, `application-prod.yml`은 정말 고정값만 남기는 것으로 정함.
- 새로 확정된 결정: 배포 환경값 대부분을 `.env`로 옮긴다.
- 남은 열린 질문: CD가 서버에 `.env`를 어떤 방식으로 써 넣을지 정해야 한다.

### Q6. CD는 서버에 `.env`를 어떻게 생성할까요?

- 막힌 결정: GitHub Actions secrets를 서버 `.env`로 옮길 때 `ssh` 안에서 직접 쓰는지, 별도 파일을 scp로 복사하는지 정해야 워크플로 변경 범위가 결정됩니다.
- 제공된 옵션:
  - 1. `(추천)` SSH 스크립트 안에서 heredoc으로 `.env`를 생성한다
  - 2. GitHub Actions에서 `.env` 파일을 만들어 SCP로 서버에 복사한다
  - 3. 서버에 미리 둔 `.env` 템플릿을 SSH로 채운다
  - 4. 다른 옵션 더 추천받기
  - 5. 직접 답변
- 답변: 1번 ㄱㄱ
- 결과: 확정
- 해석 결과: SSH 스크립트 안에서 heredoc으로 서버의 `.env`를 생성하는 방식으로 채택함.
- 새로 확정된 결정: `cd.yml`의 SSH 단계에서 `.env`를 직접 생성한다.
- 남은 열린 질문: 실제 `.env`에 넣을 키 목록을 최종 확정해야 한다.

### Q7. 서버 `.env`에 어떤 키들을 넣을까요?

- 막힌 결정: 키 목록이 정해지지 않으면 `cd.yml`에서 어떤 secret을 읽어 `.env`에 쓸지, `application-prod.yml`에서 어떤 placeholder를 남길지 확정할 수 없습니다.
- 제공된 옵션:
  - 1. `(추천)` 현재 prod 설정에 있는 secret성 값만 전부 `.env`에 둔다
  - 2. secret성 값에 더해 DB/Redis host와 app URL까지 `.env`에 둔다
  - 3. secret성 값 + 배포 환경값 + 일부 고정 설정까지 넓게 `.env`에 둔다
  - 4. 다른 옵션 더 추천받기
  - 5. 직접 답변
- 답변: 2번
- 결과: 확정
- 해석 결과: secret성 값에 더해 DB/Redis host와 app URL까지 `.env`에 둔다.
- 새로 확정된 결정: DB/Redis host와 app URL을 `.env`로 외부화한다.
- 남은 열린 질문: OAuth redirect URI, CORS origin, public key, client-id 같은 비비밀 설정의 경계를 정해야 한다.

### Q8. OAuth redirect URI와 CORS origin은 어디에 둘까요?

- 막힌 결정: app URL은 `.env`로 보냈지만, Google/Discord redirect URI와 CORS origin을 파일에 남길지 아니면 같이 환경변수화할지 정해야 실제 설정 항목이 확정됩니다.
- 제공된 옵션:
  - 1. `(추천)` OAuth redirect URI와 CORS origin도 `.env`로 둔다
  - 2. OAuth redirect URI와 CORS origin은 `application-prod.yml`에 남긴다
  - 3. redirect URI만 `.env`, CORS origin은 파일에 둔다
  - 4. 다른 옵션 더 추천받기
  - 5. 직접 답변
- 답변: 1번
- 결과: 확정
- 해석 결과: OAuth redirect URI와 CORS origin도 `.env`로 외부화하는 것으로 확정함.
- 새로 확정된 결정: redirect URI와 CORS origin을 `.env`로 둔다.
- 남은 열린 질문: client-id, public key, subject 같은 비밀 아닌 값의 경계를 정해야 한다.

### Q9. client-id와 공개 키 같은 비비밀 값은 어디에 둘까요?

- 막힌 결정: Google/Discord client-id, WebPush public key, mail subject 같은 값이 환경별로 변할 수 있으니 파일에 둘지 `.env`로 둘지 정해야 최종 스키마가 완성됩니다.
- 제공된 옵션:
  - 1. `(추천)` client-id와 공개 키, subject는 `application-prod.yml`에 남긴다
  - 2. client-id와 공개 키, subject도 `.env`로 둔다
  - 3. client-id는 `.env`, 공개 키와 subject는 파일에 둔다
  - 4. 다른 옵션 더 추천받기
  - 5. 직접 답변
- 답변: 2
- 결과: 확정
- 해석 결과: client-id와 공개 키, subject까지 `.env`로 외부화하는 것으로 확정함.
- 새로 확정된 결정: Google/Discord client-id, WebPush public key, mail subject도 `.env`로 둔다.
- 남은 열린 질문: 누락된 환경변수가 있을 때 배포와 애플리케이션이 어떻게 실패해야 하는지 정해야 한다.

### Q10. 누락된 환경변수는 어떻게 처리할까요?

- 막힌 결정: `application-prod.yml`에 기본값을 두지 않기로 했기 때문에, 필수 env가 비어 있을 때 배포를 막을지 애플리케이션 startup에서 즉시 실패할지 정해야 운영 사고를 막을 수 있습니다.
- 제공된 옵션:
  - 1. `(추천)` CD 단계에서 필수 env가 비어 있으면 배포를 중단한다
  - 2. 애플리케이션 startup에서 실패하게 두고 CD는 그대로 진행한다
  - 3. 둘 다 한다: CD에서 검사하고 앱도 startup에서 실패한다
  - 4. 다른 옵션 더 추천받기
  - 5. 직접 답변
- 답변: 1
- 결과: 확정
- 해석 결과: CD 단계에서 필수 env가 비어 있으면 배포를 중단하는 것으로 확정함.
- 새로 확정된 결정: 비어 있는 필수 env가 있으면 배포를 막는다.
- 남은 열린 질문: 배포 전 검사에서 어떤 키 집합을 필수로 볼지 최종 확정해야 한다.

### Q11. CD가 필수로 검사할 키 집합은 무엇일까요?

- 막힌 결정: 필수 키 목록이 정해지지 않으면 heredoc `.env` 생성 시 어떤 항목을 반드시 채워야 하는지와 검증 실패 조건이 모호해집니다.
- 제공된 옵션:
  - 1. `(추천)` 현재 `.env`로 옮긴 모든 키를 필수로 검사한다
  - 2. 로그인/DB/API 관련 핵심 키만 필수로 검사하고 나머지는 선택으로 둔다
  - 3. 아주 최소한의 키만 필수로 검사하고 나머지는 앱 startup에 맡긴다
  - 4. 다른 옵션 더 추천받기
  - 5. 직접 답변
- 답변: 1
- 결과: 확정
- 해석 결과: `.env`로 옮긴 모든 키를 CD가 필수로 검사하는 것으로 확정함.
- 새로 확정된 결정: 배포 전 검사에서 `.env` 전체 키를 필수 검증한다.
- 남은 열린 질문: 없음에 가깝다. 구현 순서만 남았다.

## 확정된 결정

- `application-prod.yml`은 비밀 없는 템플릿으로 유지하고, 기본값 없이 `${VAR}` 치환만 사용한다.
- 비밀값, URL/redirect/CORS, DB/Redis host, app URL, client-id, public key, subject를 모두 `.env`로 옮긴다.
- CD는 SSH 단계에서 heredoc으로 서버의 `.env`를 직접 생성한다.
- CD는 배포 전에 `.env`에 필요한 모든 키가 채워졌는지 검사하고, 하나라도 비면 배포를 중단한다.

## 임시 가정

- 현재 CD는 `ssh-action` 안에서 `.env` 생성과 배포 검증을 함께 처리할 수 있다.

## 남은 known unknowns

- 없음에 가깝다. 실제 구현만 남았다.

## 최종 정리

`ISSUE-030`은 `cd.yml`에서 서버 `.env`를 heredoc으로 생성하고, `application-prod.yml`은 기본값 없는 템플릿으로 유지한 뒤, 운영 비밀과 배포 환경값 대부분을 `.env`로 옮기는 방향으로 정리됐다. CD 단계에서 필수 키 누락을 막아야 하며, 배포 전 검증이 마지막 안전장치가 된다.

## 확정된 결정

- `application-prod.yml`에 평문 운영 비밀값을 남기지 않는 방향이 필요하다.
- GitHub Actions secrets를 서버 실행 환경으로 전달하고, `docker compose`가 환경변수 치환으로 읽는 모델을 채택한다.
- 배포 시 서버에 `.env` 파일을 생성하고 `docker compose`가 이를 읽는다.
- `application-prod.yml`은 비밀 없는 템플릿으로 유지하고, `${VAR}` 치환만 사용한다.
- 비밀값은 `.env`, 파일 경로/고정 설정은 `application-prod.yml`에 둔다.

## 임시 가정

- 현재 CD는 파일 생성형 secret 주입에 의존하고 있다.
- 서버에 `.env` 파일이 생성되어 `docker compose`가 이를 읽는다.

## 남은 known unknowns

- 어떤 비밀값은 env var로, 어떤 것은 파일로 둘지 정해야 한다.

## 최종 정리

[진행 중]
