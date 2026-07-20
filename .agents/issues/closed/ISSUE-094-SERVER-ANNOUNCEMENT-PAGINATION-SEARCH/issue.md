# [P1][server] 공지사항 public/admin 목록·검색을 bounded pagination으로 변경

## 문제

공지사항 repository에는 pageable overload가 있지만 `AnnouncementService`는 사용하지 않고 public 검색·admin 전체 조회·title/content 검색 모두 `List`를 반환한다. 데이터가 늘면 한 번의 요청이 전체 본문 preview/detail을 메모리에 올린다.

## 완료 기준

- public list/search와 admin list가 Page/Slice 및 고정 최대 page size를 사용한다.
- keyword 길이·검색 type을 검증하고 빈 검색어에도 상한을 적용한다.
- title/content 검색에 인덱스 또는 검색 비용을 관리할 계획이 있고 query latency metric이 있다.
- 기존 frontend pagination과 pinned ordering을 보존하는 API contract test가 있다.

## 근거

- `apps/server/src/main/java/bhoon/sugang_helper/announcement/application/AnnouncementService.java`
- `apps/server/src/main/java/bhoon/sugang_helper/announcement/presentation/AnnouncementController.java`
- `apps/server/src/main/java/bhoon/sugang_helper/admin/presentation/AdminAnnouncementController.java`
