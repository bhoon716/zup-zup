# 주요 기능 및 API 업데이트 (Feature Updates - Server)

이 문서는 `server` 모듈의 버전별 주요 기능 추가 및 API 변경 사항을 기록합니다.

---

## API 변경 사항 (v1.1)

### 강의 검색 페이징 (Pagination)

- **Endpoint**: `GET /api/v1/courses`
- **Response**: `Slice<CourseResponse>` 구조로 변경되어 무한 스크롤에 최적화된 데이터를 반환합니다.

---

## API 변경 사항 (v1.2)

### 응답 데이터 현지화 및 코드 품질 개선 (Localization & Cleanup)

- **Enum 현지화**: `TimetableCourseResponse` 등에서 이수구분(`classification`) 필드 반환 시 Enum 명칭 대신 한글 설명(`getDescription()`)을 반환하도록 수정하여 프론트엔드에서의 추가 포맷팅 부담을 줄였습니다.
- **코드 스타일 표준화**: 프로젝트 전반의 Java 소스 코드 들여쓰기(Indentation)를 4개 공백으로 통일하고 불필요한 임포트 및 주석을 정리하여 클린 코드를 실천했습니다.
- **안정성 강화**: 과목 상세 조회 API(`GET /api/v1/courses/{courseKey}`)의 응답 구조를 명확히 하고 Swagger 문서를 최신화했습니다.
