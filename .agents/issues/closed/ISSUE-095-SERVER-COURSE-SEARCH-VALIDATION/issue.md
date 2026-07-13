# [P2][server] 과목 검색 조건 DTO의 schema·list·enum 입력 검증

## 문제

`CourseSearchController`는 `@Valid` 없이 `CourseSearchCondition`을 받고, 조건 DTO에 문자열 길이·list 개수·선택 시간표·학점 범위 제약이 없다. Query 구현은 잘못된 enum 값을 조용히 무시해 사용자가 의도하지 않은 넓은 검색을 받을 수 있다.

## 완료 기준

- request DTO에 `@Valid`, 허용 길이·list size·시간/학점 범위·enum 검증을 추가한다.
- `sortBy/sortOrder`는 whitelist와 기본값을 명시한다.
- 잘못된 값은 4xx로 거부하고 조용한 필터 무시는 제거한다.
- 대형 배열·긴 문자열·잘못된 enum의 API 테스트가 있다.

## 근거

- `apps/server/src/main/java/bhoon/sugang_helper/course/presentation/CourseController.java`
- `apps/server/src/main/java/bhoon/sugang_helper/course/application/CourseSearchCondition.java`
- `apps/server/src/main/java/bhoon/sugang_helper/course/infra/CourseJpaRepositoryImpl.java`

## 검증 로그

- `CourseControllerValidationTest`: oversized name과 잘못된 target grade는 400이며 service를 호출하지 않는다.
- `CourseSearchConditionValidationTest`: list/sort 제약과 enum/credit 값 검증을 확인했다.
- `./gradlew check --no-daemon` 통과.
