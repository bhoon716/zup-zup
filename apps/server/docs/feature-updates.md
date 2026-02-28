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

---

## API 변경 사항 (v1.3)

### 다년도/다학기 크롤링 관리 및 요청 안정성 개선 (Crawl Management & Stability)

- **크롤링 타겟 유연화**: `CrawlerSetting` 엔티티와 `CourseCrawlerTargetService`를 도입하여, 스케줄러 및 기본 크롤링에서 사용할 년도와 학기를 DB에서 관리할 수 있게 되었습니다.
- **관리자 전용 API**:
  - `GET /api/v1/admin/courses/crawl-target`: 현재 설정된 기본 크롤링 타겟 조회
  - `PUT /api/v1/admin/courses/crawl-target`: 기본 크롤링 타겟 수정 및 저장
  - `POST /api/v1/admin/courses/crawl/target`: 특정 년도/학기를 대상으로 즉시 크롤링 실행
- **API 호출 안정성**: 오아시스 서버의 응답 지연에 대응하기 위해 타임아웃(60s~120s)과 재시도(1~2회) 메커니즘을 `application.yml` 설정을 통해 동적으로 조절 가능하도록 개선했습니다.
- **스케줄러 고도화**: 크롤링 주기를 Cron 표현식으로 관리하며, 실패 시 상세 원인을 로그로 기록하도록 에러 핸들링을 강화했습니다.
