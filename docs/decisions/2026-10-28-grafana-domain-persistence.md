# Grafana 도메인 설정의 저장소 영속화

## 문제와 확정 범위

사용자는 Grafana를 `grafana.zup-zup.com`으로 이전하고 HTTPS·Grafana 주소 설정을 적용하도록 승인했다. 운영 이전 작업은 별도 수행됐으며 기존 DuckDNS 경로는 유지한다. 운영 Compose에만 추가한 설정은 다음 서버 배포가 저장소의 Compose를 복사할 때 사라질 수 있다.

이번 변경은 해당 네 설정을 `infra/docker-compose.yml`에 보존한다. DNS·NPM·인증서·계정·자격 증명·포트·네트워크·대시보드·DB migration은 변경하지 않는다. DuckDNS 삭제와 Clarity 추가 마스킹 해제는 범위 밖이다.

## 선택과 근거

- `GF_SERVER_DOMAIN=grafana.zup-zup.com`
- `GF_SERVER_ROOT_URL=https://grafana.zup-zup.com/`
- `GF_SECURITY_COOKIE_SECURE=true`
- `GF_LIVE_ALLOWED_ORIGINS=https://grafana.zup-zup.com,https://jbnu-sugang-helper-grafana.duckdns.org`

운영 서버에서만 설정을 유지하는 대안은 정상 CD의 파일 교체에 취약하다. 별도 override·새 secret·추가 환경변수 계약을 만드는 대신 이미 적용된 비밀이 아닌 값을 기존 Compose에 그대로 기록한다. Grafana의 localhost 포트 공개, 이미지 digest, 기존 볼륨, 가입 차단과 보안 옵션은 유지한다. Live origin은 두 HTTPS origin만 명시하며 wildcard는 추가하지 않는다.

## 검증과 운영 적용

렌더된 Compose의 네 설정을 기존 observability 계약 테스트에서 검증한다. 기존 runtime·CD·release·로컬 Compose 계약도 검증하며 앱/웹 소스는 바꾸지 않는다. 전달받은 패치는 배포 시 `docker-compose.yml`로 복사되는 저장소의 `infra/docker-compose.yml`에 대응한다.

사전 직접 SSH 조회는 연결 시간 초과로 실패했다. 따라서 운영의 네 값·Grafana healthy 확인은 도메인 이전 담당자의 `grafana-migration-execution-result.md` 기록에 근거하며, 이번 작업의 신규 실측과 구분한다. 정상 PR·CI 후 기존 CD로 배포하고, 가능해지면 실행 컨테이너의 허용된 네 값과 새/기존 주소 health를 다시 확인한다. 전체 환경변수·비밀·쿠키는 출력하지 않는다.

## 비용과 복구

기존 CD는 infra 변경에도 서버 이미지를 빌드하고 관측 컨테이너 재생성·앱 재시작·기존 migration 실행을 수행한다. 이 동작을 이번 변경에서 재설계하지 않는다. 새 migration 파일이나 데이터 삭제는 없다.

원래 코드로 단순 rollback하면 이미 운영에 적용한 네 설정도 다음 CD에서 제거되므로 도메인 이전 rollback과 구분한다. 앱 rollback이 필요하면 이 네 설정을 보존한 상태로 이전 검증 이미지를 선택해야 한다. 도메인 이전 자체를 되돌리는 경우에만 담당자가 확보한 서버 백업과 기존 DuckDNS 경로를 사용한다. Grafana 데이터 볼륨 복원이나 DB 복원은 이 설정 변경의 정상 복구에 필요하지 않다.

새 도메인은 기존 DuckDNS와 cookie 범위가 다르므로 사용자 재로그인이 필요할 수 있다. 브라우저 인증·Live WebSocket의 새 도메인 검증은 도메인 담당 작업과 조율한다.

## 로컬 검증 결과

2026-10-28에 셸 문법 검사와 runtime·observability·CD workflow·앱 환경 사전 검사·release layout·로컬 이미지 흐름·로컬 Compose·백업·Redis 인증 복구의 9종 계약 검사가 통과했다. Mac의 `python` 명령이 없어 처음 실패한 3종은 저장소/전역 환경을 바꾸지 않고 작업 전용 PATH의 python3 연결로 재검증했다. 테스트 삭제나 검증 완화는 없다.

운영 공개 HTTPS의 새 주소와 기존 DuckDNS `/api/health`를 직접 조회해 HTTP 200, database=ok, Grafana 13.1.0을 확인했다. 이 결과는 로그인 이후 대시보드·쿠키·Live WebSocket 검증을 대신하지 않는다. 원래 dirty 문서 3개의 해시와 index 상태도 보존했다.
