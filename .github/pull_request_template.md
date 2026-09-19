## 변경 요약

<!-- 무엇을 왜 변경했는지 1~3문장으로 작성해 주세요. -->

## 관련 이슈

- Closes #

## 변경 범위

- [ ] Server (`apps/server`)
- [ ] Web (`apps/web`)
- [ ] Infra/CI (`infra`, `.github`)
- [ ] 문서

## 주요 변경 사항

-

## 검증

<!-- 실행한 명령과 결과를 적어 주세요. 해당하지 않는 항목은 선택하지 않아도 됩니다. -->

- [ ] Server: `cd apps/server && ./gradlew clean check --no-daemon`
- [ ] Server migration: `cd apps/server && ./gradlew migrationTest --no-daemon`
- [ ] Web: `cd apps/web && npm run lint && npm run test -- --run && npm run build`
- [ ] Infra 계약/스크립트 검증
- [ ] 수동 검증 또는 운영 smoke test

검증 결과:

## 데이터베이스·배포 영향

- DB migration 변경: 없음 / 있음 (버전: `V__...`)
- 환경변수·secret 변경: 없음 / 있음 (값 자체는 적지 마세요)
- 배포 시 주의사항:
- rollback 방법:

## 리뷰어가 확인할 내용

-

## 작성자 체크리스트

- [ ] 관련 이슈와 변경 범위를 확인했다.
- [ ] 실패하던 동작 또는 기대 동작을 테스트로 검증했다.
- [ ] 민감정보·토큰·개인정보가 로그, 응답, 커밋에 포함되지 않았다.
- [ ] 불필요한 리팩터링·포맷 변경을 포함하지 않았다.
- [ ] 문서·migration·배포 절차 변경이 필요한 경우 함께 반영했다.
