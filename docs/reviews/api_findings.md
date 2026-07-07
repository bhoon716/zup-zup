# API 진단 결과 (API Findings)

## 1. 일관적이지 않은 REST API 반환 규격 (API-001)
* **상세**: `AdminFeedbackController` 및 `UserController` 내 일부 콜백 엔드포인트 메서드들은 `CommonResponse`로 원소 데이터를 포장하지 않고 직접적으로 응답 모델을 노출하고 있습니다.
* **영향**: Next.js Axios 가상 통신 규격에서 공통 인터셉터 레벨 처리가 번거로워지고, 규격 예외 케이스 처리 코드가 프론트에 늘어납니다.
* **조치 제안**: 컨트롤러 엔드포인트 응답 타입을 `ResponseEntity<CommonResponse<T>>`로 전면적으로 일치시켜 일관성을 수립하십시오.

## 2. API 파라미터 DTO 유효성 검증(Validation) 부재
* **상세**: 건의사항 피드백 작성 등 외부 문자열 입력에 매핑되는 일부 Presentation DTO 내부 필드에 최소/최대 길이 체크나 미입력 공백 처리 방어 코드(`@NotBlank`, `@Size` 등)가 미흡합니다.
* **영향**: 비정상적 긴 페이로드가 유입될 때 백엔드 비즈니스 로직 연산에서 예외를 던지며 500 internal server error 혹은 DB 저장 오류로 전환될 수 있습니다.
* **조치 제안**: 데이터 유입 초입부인 DTO 명세에 Validation 검증 어노테이션을 밀도 있게 추가하여 정합성을 정제해 주십시오.
