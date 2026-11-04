# 내부 서비스 인터페이스 정리

단일 구현의 전달 계약 21개를 제거하고 기존 서비스 19개를 직접 주입한다. 서비스 메서드·업무 본문·트랜잭션 애노테이션·빈 의존 관계는 유지하고 implements/@Override·타입·import만 정리했다. 새 계층이나 변환 객체는 추가하지 않는다.

| 제거한 인터페이스 | 기존 구체 서비스 |
| --- | --- |
| NotificationTestSender | [NotificationService](../apps/server/src/main/java/bhoon/sugang_helper/seatalert/notification/service/NotificationService.java) |
| NotificationHistoryQuery | [NotificationHistoryQueryService](../apps/server/src/main/java/bhoon/sugang_helper/seatalert/notification/service/NotificationHistoryQueryService.java) |
| CrawlerTargetQuery | [CourseCrawlerTargetService](../apps/server/src/main/java/bhoon/sugang_helper/academic/crawling/service/CourseCrawlerTargetService.java) |
| CrawlerAdminSnapshotQuery | [CourseCrawlerTargetService](../apps/server/src/main/java/bhoon/sugang_helper/academic/crawling/service/CourseCrawlerTargetService.java) |
| CrawlerStatusQuery | [CrawlerRunStateService](../apps/server/src/main/java/bhoon/sugang_helper/academic/crawling/service/CrawlerRunStateService.java) |
| CourseSearchContextProvider | [CrawlerCourseSearchContextProvider](../apps/server/src/main/java/bhoon/sugang_helper/academic/crawling/service/CrawlerCourseSearchContextProvider.java) |
| CourseReviewQuery | [CourseReviewQueryService](../apps/server/src/main/java/bhoon/sugang_helper/academic/review/service/CourseReviewQueryService.java) |
| PrimaryTimetableQuery | [TimetableService](../apps/server/src/main/java/bhoon/sugang_helper/planning/timetable/service/TimetableService.java) |
| AdminAuditRecorder | [AdminAuditService](../apps/server/src/main/java/bhoon/sugang_helper/admin/audit/service/AdminAuditService.java) |
| DashboardAnnouncementQuery | [AnnouncementService](../apps/server/src/main/java/bhoon/sugang_helper/campus/announcement/service/AnnouncementService.java) |
| UpcomingScheduleQuery | [ScheduleService](../apps/server/src/main/java/bhoon/sugang_helper/campus/schedule/service/ScheduleService.java) |
| UserDeviceWebPushManagement | [UserDeviceService](../apps/server/src/main/java/bhoon/sugang_helper/identity/user/service/UserDeviceService.java) |
| UserDashboardQuery | [UserDashboardQueryService](../apps/server/src/main/java/bhoon/sugang_helper/identity/user/service/UserDashboardQueryService.java) |
| UserDisplayNameQuery | [UserDisplayNameQueryService](../apps/server/src/main/java/bhoon/sugang_helper/identity/user/service/UserDisplayNameQueryService.java) |
| OAuthAccountProvisioner | [UserOAuthAccountService](../apps/server/src/main/java/bhoon/sugang_helper/identity/user/service/UserOAuthAccountService.java) |
| UserSessionLogout | [AuthService](../apps/server/src/main/java/bhoon/sugang_helper/identity/auth/service/AuthService.java) |
| UserNotificationTestSender | [NotificationService](../apps/server/src/main/java/bhoon/sugang_helper/seatalert/notification/service/NotificationService.java) |
| UserNotificationQuery | [UserNotificationQueryService](../apps/server/src/main/java/bhoon/sugang_helper/identity/user/service/UserNotificationQueryService.java) |
| UserIdentityQuery | [UserIdentityQueryService](../apps/server/src/main/java/bhoon/sugang_helper/identity/user/service/UserIdentityQueryService.java) |
| UserFeedbackCleanup | [FeedbackAccountLifecycleService](../apps/server/src/main/java/bhoon/sugang_helper/support/feedback/service/FeedbackAccountLifecycleService.java) |
| UserSubscriptionCleanup | [SubscriptionAccountLifecycleService](../apps/server/src/main/java/bhoon/sugang_helper/seatalert/subscription/service/SubscriptionAccountLifecycleService.java) |

## 유지한 계약

- NotificationSender: Email/Discord/FCM/WebPush 네 구현을 선택하는 실제 발송 경계다.
- CourseApiClient·SlackWebhookClient: 외부 HTTP 호출을 로컬 대체 제공자와 분리하는 경계다.
- Spring Data repository, OAuth2UserService·TaskDecorator·AutoCloseable 등 프레임워크 계약, 실제 여러 메타데이터 타입이 구현하는 sealed interface는 유지했다.

서버 전체 123 suite·572건, 실패/오류/skip 0, PMD·Checkstyle main/test 위반 0, check bootJar 통과. 테스트의 기존 정상·오류·취소·재시도·거래 검증은 제거하지 않고 mock 타입만 구체 서비스로 바꿨다.
