# 성능 진단 결과 (Performance Findings)

## 1. 조인 쿼리 복합 인덱스 누락 (PERF-001)
* **위치**: [CourseReviewJpaRepository.java](file:///Users/bhoon/Project/jbnu-sugang-helper/server/src/main/java/bhoon/sugang_helper/review/infra/CourseReviewJpaRepository.java)
* **상세**: `findBySubjectCodeAndProfessorWithMyReviewFirst` 등의 조회 동작 시 과목 정보인 `courses` 테이블을 조인합니다.
  ```sql
  join Course c on c.courseKey = r.courseKey
  where c.subjectCode = :subjectCode and coalesce(trim(c.professor), '') = :professor
  ```
* **영향**: 대량의 코스 정보와 리뷰가 적재될수록 텍스트 비교 조인에 인덱스가 동작하지 않아 Full Table Scan 및 Full Index Scan이 진행되므로 응답 지연을 초래합니다.
* **조치 제안**: `courses` 테이블에 `(subject_code, professor)` 복합 인덱스 생성을 마이그레이션 스크립트에 반영하십시오.

## 2. 초기 기동 시 크롤링 작업 동기 처리 부하
* **위치**: [CourseScheduler.java](file:///Users/bhoon/Project/jbnu-sugang-helper/server/src/main/java/bhoon/sugang_helper/crawling/application/CourseScheduler.java)
* **상세**: `runOnStartup` 활성화 상태 시 서버 구동 이벤트 리스너에서 직접 3개년치 전북대학교 강의 정보 동기 크롤링을 동작시킵니다.
* **영향**: WAS 구동이 지연되거나 타임아웃되어 헬스체크 실패로 컨테이너가 반복 재부팅될 위험이 있습니다.
* **조치 제안**: 시작 이벤트 크롤링 실행 로직을 비동기(`@Async`)로 격리 처리하여 WAS 구동과 분리하십시오.

## 3. 단순 조회용 비즈니스 로직 트랜잭션 최적화 누락 (PERF-003)
* **위치**: [UserService.java](file:///Users/bhoon/Project/jbnu-sugang-helper/server/src/main/java/bhoon/sugang_helper/user/application/UserService.java#L211)
* **상세**: `getCurrentUser` 메서드가 단순 조회 동작을 담당하지만, 서비스 클래스 레벨의 `@Transactional(readOnly = true)`가 보장하는 더티 체킹(Dirty Checking) 생략 효과를 얻기 위해 명시적으로 개별적인 `readOnly` 설정을 고려해야 합니다. (현재 클래스 레벨에 readOnly=true가 선언되어 있으므로 안전하게 이행 중인 상태이나 개별 커스텀 쿼리 및 리포지토리 레이어에서의 JPA 영속성 컨텍스트 플러시 오버헤드를 계속 관리해야 합니다.)

## 4. 대용량 페이지네이션 메모리 과부하 리스크 (PERF-004)
* **위치**: [FeedbackService.java](file:///Users/bhoon/Project/jbnu-sugang-helper/server/src/main/java/bhoon/sugang_helper/feedback/application/FeedbackService.java) 및 [CourseReviewJpaRepository.java](file:///Users/bhoon/Project/jbnu-sugang-helper/server/src/main/java/bhoon/sugang_helper/review/infra/CourseReviewJpaRepository.java)
* **상세**: 수강평 목록 및 피드백 목록을 무제한성 대용량으로 가져오는 경우, 적절한 페이지 크기 한도가 강제되지 않으면 OutOfMemory 리스크가 있습니다.
* **영향**: 한 번에 너무 큰 페이지의 엔티티가 영속성 컨텍스트에 로드되어 GC(Garbage Collector)의 오버헤드를 유발하고 인프라 중단(OOM)으로 이어질 수 있습니다.
* **조치 제안**: 컨트롤러 유입 단에서 `Pageable`의 최대 조회 팩터(Max Page Size)를 50 이하로 강제 제한하는 밸리데이터를 추가하십시오.

