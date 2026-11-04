# DTO 역할 분류

API 입력은 request, API 직렬화 출력은 response, 서비스·조회 내부 결과는 result에 둔다. 클래스명·필드·검증·Jackson 설정은 유지하고 DTO 복제나 변환 계층을 추가하지 않았다. 이름이 Result여도 직접 JSON 필드로 노출되는 Dashboard 타입은 response다.

| 역할 | 타입 수 |
| --- | ---: |
| command | 6 |
| criteria | 4 |
| diagnostic | 1 |
| external | 2 |
| provider | 1 |
| request | 26 |
| response | 53 |
| result | 28 |

## 공유·별도 역할의 판단

- ScheduleSummary·AnnouncementPreview·CrawlerAdminSnapshot과 시간표의 중첩 Snapshot 네 타입은 대시보드에 직접 직렬화되므로 response다. PrimaryTimetableSnapshot과 User 결과는 API 응답으로 변환되므로 result다.
- 관리자 감사 메타데이터는 API와 DB JSON이 공유하는 기존 타입을 response에 그대로 둔다. 이 모델의 업무 data 필드는 성공 공통 래퍼와 다르므로 유지한다.
- command는 서비스 입력, criteria는 조회 조건·범위 키, external은 외부 수집 결과, provider는 외부 발송 대상, diagnostic은 실패 진단 모델이다.
- LoadedFile·FeedbackAttachmentDownload는 JSON 응답 모델이 아니라 컨트롤러가 Resource·헤더를 구성할 때 사용하는 내부 결과다.

## 타입 목록

### command

- [CompleteOnboardingCommand](../apps/server/src/main/java/bhoon/sugang_helper/identity/user/dto/command/CompleteOnboardingCommand.java)
- [RegisterDeviceCommand](../apps/server/src/main/java/bhoon/sugang_helper/identity/user/dto/command/RegisterDeviceCommand.java)
- [SendVerificationCodeCommand](../apps/server/src/main/java/bhoon/sugang_helper/identity/user/dto/command/SendVerificationCodeCommand.java)
- [UpdateProfileCommand](../apps/server/src/main/java/bhoon/sugang_helper/identity/user/dto/command/UpdateProfileCommand.java)
- [UpdateSettingsCommand](../apps/server/src/main/java/bhoon/sugang_helper/identity/user/dto/command/UpdateSettingsCommand.java)
- [VerifyEmailCommand](../apps/server/src/main/java/bhoon/sugang_helper/identity/user/dto/command/VerifyEmailCommand.java)

### criteria

- [CourseSearchCriteria](../apps/server/src/main/java/bhoon/sugang_helper/academic/course/dto/criteria/CourseSearchCriteria.java)
- [CourseSelectedSchedule](../apps/server/src/main/java/bhoon/sugang_helper/academic/course/dto/criteria/CourseSelectedSchedule.java)
- [ReviewScopeKey](../apps/server/src/main/java/bhoon/sugang_helper/academic/review/dto/criteria/ReviewScopeKey.java)
- [FeedbackSearchCondition](../apps/server/src/main/java/bhoon/sugang_helper/support/feedback/dto/criteria/FeedbackSearchCondition.java)

### diagnostic

- [CrawlerFailureSummary](../apps/server/src/main/java/bhoon/sugang_helper/academic/crawling/dto/diagnostic/CrawlerFailureSummary.java)

### external

- [ParsedCourseDto](../apps/server/src/main/java/bhoon/sugang_helper/academic/course/dto/external/ParsedCourseDto.java)
- [ParsedCourseScheduleDto](../apps/server/src/main/java/bhoon/sugang_helper/academic/course/dto/external/ParsedCourseScheduleDto.java)

### provider

- [NotificationTarget](../apps/server/src/main/java/bhoon/sugang_helper/seatalert/notification/dto/provider/NotificationTarget.java)

### request

- [CourseSearchCondition](../apps/server/src/main/java/bhoon/sugang_helper/academic/course/dto/request/CourseSearchCondition.java)
- [ScheduleCondition](../apps/server/src/main/java/bhoon/sugang_helper/academic/course/dto/request/ScheduleCondition.java)
- [AdminCrawlTargetRequest](../apps/server/src/main/java/bhoon/sugang_helper/academic/crawling/dto/request/AdminCrawlTargetRequest.java)
- [ReviewCreateRequest](../apps/server/src/main/java/bhoon/sugang_helper/academic/review/dto/request/ReviewCreateRequest.java)
- [ReviewReactionRequest](../apps/server/src/main/java/bhoon/sugang_helper/academic/review/dto/request/ReviewReactionRequest.java)
- [ReviewUpdateRequest](../apps/server/src/main/java/bhoon/sugang_helper/academic/review/dto/request/ReviewUpdateRequest.java)
- [AnnouncementRequest](../apps/server/src/main/java/bhoon/sugang_helper/campus/announcement/dto/request/AnnouncementRequest.java)
- [ScheduleRequest](../apps/server/src/main/java/bhoon/sugang_helper/campus/schedule/dto/request/ScheduleRequest.java)
- [EmailRequest](../apps/server/src/main/java/bhoon/sugang_helper/identity/user/dto/request/EmailRequest.java)
- [EmailVerificationRequest](../apps/server/src/main/java/bhoon/sugang_helper/identity/user/dto/request/EmailVerificationRequest.java)
- [OnboardingRequest](../apps/server/src/main/java/bhoon/sugang_helper/identity/user/dto/request/OnboardingRequest.java)
- [UserDeviceRequest](../apps/server/src/main/java/bhoon/sugang_helper/identity/user/dto/request/UserDeviceRequest.java)
- [UserDeviceUnregisterRequest](../apps/server/src/main/java/bhoon/sugang_helper/identity/user/dto/request/UserDeviceUnregisterRequest.java)
- [UserSettingsRequest](../apps/server/src/main/java/bhoon/sugang_helper/identity/user/dto/request/UserSettingsRequest.java)
- [UserUpdateRequest](../apps/server/src/main/java/bhoon/sugang_helper/identity/user/dto/request/UserUpdateRequest.java)
- [CustomScheduleRequest](../apps/server/src/main/java/bhoon/sugang_helper/planning/timetable/dto/request/CustomScheduleRequest.java)
- [CustomScheduleTimeRequest](../apps/server/src/main/java/bhoon/sugang_helper/planning/timetable/dto/request/CustomScheduleTimeRequest.java)
- [TimetableCourseRequest](../apps/server/src/main/java/bhoon/sugang_helper/planning/timetable/dto/request/TimetableCourseRequest.java)
- [TimetableRequest](../apps/server/src/main/java/bhoon/sugang_helper/planning/timetable/dto/request/TimetableRequest.java)
- [NotificationDeliveryReplayRequest](../apps/server/src/main/java/bhoon/sugang_helper/seatalert/notification/dto/request/NotificationDeliveryReplayRequest.java)
- [SubscriptionRequest](../apps/server/src/main/java/bhoon/sugang_helper/seatalert/subscription/dto/request/SubscriptionRequest.java)
- [AdminFeedbackAttachmentDownloadRequest](../apps/server/src/main/java/bhoon/sugang_helper/support/feedback/dto/request/AdminFeedbackAttachmentDownloadRequest.java)
- [FeedbackCreateRequest](../apps/server/src/main/java/bhoon/sugang_helper/support/feedback/dto/request/FeedbackCreateRequest.java)
- [FeedbackReplyCreateRequest](../apps/server/src/main/java/bhoon/sugang_helper/support/feedback/dto/request/FeedbackReplyCreateRequest.java)
- [FeedbackReplyUpdateRequest](../apps/server/src/main/java/bhoon/sugang_helper/support/feedback/dto/request/FeedbackReplyUpdateRequest.java)
- [FeedbackStatusUpdateRequest](../apps/server/src/main/java/bhoon/sugang_helper/support/feedback/dto/request/FeedbackStatusUpdateRequest.java)

### response

- [CsrfTokenResponse](../apps/server/src/main/java/bhoon/sugang_helper/identity/auth/dto/response/CsrfTokenResponse.java)
- [CourseCategoryResponse](../apps/server/src/main/java/bhoon/sugang_helper/academic/course/dto/response/CourseCategoryResponse.java)
- [CourseDetailResponse](../apps/server/src/main/java/bhoon/sugang_helper/academic/course/dto/response/CourseDetailResponse.java)
- [CourseResponse](../apps/server/src/main/java/bhoon/sugang_helper/academic/course/dto/response/CourseResponse.java)
- [CourseSeatHistoryResponse](../apps/server/src/main/java/bhoon/sugang_helper/academic/course/dto/response/CourseSeatHistoryResponse.java)
- [ScheduleResponse](../apps/server/src/main/java/bhoon/sugang_helper/academic/course/dto/response/ScheduleResponse.java)
- [AdminCrawlTargetResponse](../apps/server/src/main/java/bhoon/sugang_helper/academic/crawling/dto/response/AdminCrawlTargetResponse.java)
- [CrawlerAdminSnapshot](../apps/server/src/main/java/bhoon/sugang_helper/academic/crawling/dto/response/CrawlerAdminSnapshot.java)
- [SearchDefaultSemesterResponse](../apps/server/src/main/java/bhoon/sugang_helper/academic/crawling/dto/response/SearchDefaultSemesterResponse.java)
- [CourseEmojiReviewResponse](../apps/server/src/main/java/bhoon/sugang_helper/academic/review/dto/response/CourseEmojiReviewResponse.java)
- [ReviewResponse](../apps/server/src/main/java/bhoon/sugang_helper/academic/review/dto/response/ReviewResponse.java)
- [AdminActionLogResponse](../apps/server/src/main/java/bhoon/sugang_helper/admin/audit/dto/response/AdminActionLogResponse.java)
- [AdminAuditMetadata](../apps/server/src/main/java/bhoon/sugang_helper/admin/audit/dto/response/AdminAuditMetadata.java)
- [AdminAuditMetadataData](../apps/server/src/main/java/bhoon/sugang_helper/admin/audit/dto/response/AdminAuditMetadataData.java)
- [ContentSummary](../apps/server/src/main/java/bhoon/sugang_helper/admin/audit/dto/response/ContentSummary.java)
- [AdminDashboardResponse](../apps/server/src/main/java/bhoon/sugang_helper/admin/dto/response/AdminDashboardResponse.java)
- [AdminDashboardSnapshotResponse](../apps/server/src/main/java/bhoon/sugang_helper/admin/dto/response/AdminDashboardSnapshotResponse.java)
- [AdminHourlyTrafficResponse](../apps/server/src/main/java/bhoon/sugang_helper/admin/dto/response/AdminHourlyTrafficResponse.java)
- [AdminOverviewResponse](../apps/server/src/main/java/bhoon/sugang_helper/admin/dto/response/AdminOverviewResponse.java)
- [AdminRecentLogResponse](../apps/server/src/main/java/bhoon/sugang_helper/admin/dto/response/AdminRecentLogResponse.java)
- [AnnouncementDetailResponse](../apps/server/src/main/java/bhoon/sugang_helper/campus/announcement/dto/response/AnnouncementDetailResponse.java)
- [AnnouncementListResponse](../apps/server/src/main/java/bhoon/sugang_helper/campus/announcement/dto/response/AnnouncementListResponse.java)
- [AnnouncementPreview](../apps/server/src/main/java/bhoon/sugang_helper/campus/announcement/dto/response/AnnouncementPreview.java)
- [ScheduleResponse](../apps/server/src/main/java/bhoon/sugang_helper/campus/schedule/dto/response/ScheduleResponse.java)
- [ScheduleSummary](../apps/server/src/main/java/bhoon/sugang_helper/campus/schedule/dto/response/ScheduleSummary.java)
- [DashboardNotificationResult](../apps/server/src/main/java/bhoon/sugang_helper/dashboard/dto/response/DashboardNotificationResult.java)
- [DashboardPrimaryTimetableResult](../apps/server/src/main/java/bhoon/sugang_helper/dashboard/dto/response/DashboardPrimaryTimetableResult.java)
- [DashboardSnapshotResponse](../apps/server/src/main/java/bhoon/sugang_helper/dashboard/dto/response/DashboardSnapshotResponse.java)
- [DashboardUserResult](../apps/server/src/main/java/bhoon/sugang_helper/dashboard/dto/response/DashboardUserResult.java)
- [UserDeviceResponse](../apps/server/src/main/java/bhoon/sugang_helper/identity/user/dto/response/UserDeviceResponse.java)
- [UserResponse](../apps/server/src/main/java/bhoon/sugang_helper/identity/user/dto/response/UserResponse.java)
- [CustomScheduleResponse](../apps/server/src/main/java/bhoon/sugang_helper/planning/timetable/dto/response/CustomScheduleResponse.java)
- [CustomScheduleTimeResponse](../apps/server/src/main/java/bhoon/sugang_helper/planning/timetable/dto/response/CustomScheduleTimeResponse.java)
- [TimetableCourseResponse](../apps/server/src/main/java/bhoon/sugang_helper/planning/timetable/dto/response/TimetableCourseResponse.java)
- [TimetableCourseScheduleResponse](../apps/server/src/main/java/bhoon/sugang_helper/planning/timetable/dto/response/TimetableCourseScheduleResponse.java)
- [TimetableCourseScheduleSnapshot](../apps/server/src/main/java/bhoon/sugang_helper/planning/timetable/dto/response/TimetableCourseScheduleSnapshot.java)
- [TimetableCourseSnapshot](../apps/server/src/main/java/bhoon/sugang_helper/planning/timetable/dto/response/TimetableCourseSnapshot.java)
- [TimetableCustomScheduleSnapshot](../apps/server/src/main/java/bhoon/sugang_helper/planning/timetable/dto/response/TimetableCustomScheduleSnapshot.java)
- [TimetableCustomScheduleTimeSnapshot](../apps/server/src/main/java/bhoon/sugang_helper/planning/timetable/dto/response/TimetableCustomScheduleTimeSnapshot.java)
- [TimetableDetailResponse](../apps/server/src/main/java/bhoon/sugang_helper/planning/timetable/dto/response/TimetableDetailResponse.java)
- [TimetableResponse](../apps/server/src/main/java/bhoon/sugang_helper/planning/timetable/dto/response/TimetableResponse.java)
- [WishlistResponse](../apps/server/src/main/java/bhoon/sugang_helper/planning/wishlist/dto/response/WishlistResponse.java)
- [WishlistToggleResponse](../apps/server/src/main/java/bhoon/sugang_helper/planning/wishlist/dto/response/WishlistToggleResponse.java)
- [AdminNotificationDeliveryResponse](../apps/server/src/main/java/bhoon/sugang_helper/seatalert/notification/dto/response/AdminNotificationDeliveryResponse.java)
- [NotificationHistoryResponse](../apps/server/src/main/java/bhoon/sugang_helper/seatalert/notification/dto/response/NotificationHistoryResponse.java)
- [SubscriptionResponse](../apps/server/src/main/java/bhoon/sugang_helper/seatalert/subscription/dto/response/SubscriptionResponse.java)
- [AdminFeedbackAttachmentResponse](../apps/server/src/main/java/bhoon/sugang_helper/support/feedback/dto/response/AdminFeedbackAttachmentResponse.java)
- [AdminFeedbackDetailResponse](../apps/server/src/main/java/bhoon/sugang_helper/support/feedback/dto/response/AdminFeedbackDetailResponse.java)
- [AdminFeedbackReplyResponse](../apps/server/src/main/java/bhoon/sugang_helper/support/feedback/dto/response/AdminFeedbackReplyResponse.java)
- [AdminFeedbackResponse](../apps/server/src/main/java/bhoon/sugang_helper/support/feedback/dto/response/AdminFeedbackResponse.java)
- [FeedbackDetailResponse](../apps/server/src/main/java/bhoon/sugang_helper/support/feedback/dto/response/FeedbackDetailResponse.java)
- [FeedbackReplyResponse](../apps/server/src/main/java/bhoon/sugang_helper/support/feedback/dto/response/FeedbackReplyResponse.java)
- [FeedbackResponse](../apps/server/src/main/java/bhoon/sugang_helper/support/feedback/dto/response/FeedbackResponse.java)

### result

- [CourseName](../apps/server/src/main/java/bhoon/sugang_helper/academic/course/dto/result/CourseName.java)
- [CourseReference](../apps/server/src/main/java/bhoon/sugang_helper/academic/course/dto/result/CourseReference.java)
- [CourseScheduleSummary](../apps/server/src/main/java/bhoon/sugang_helper/academic/course/dto/result/CourseScheduleSummary.java)
- [CourseSearchContext](../apps/server/src/main/java/bhoon/sugang_helper/academic/course/dto/result/CourseSearchContext.java)
- [CourseSummary](../apps/server/src/main/java/bhoon/sugang_helper/academic/course/dto/result/CourseSummary.java)
- [CrawlTargetInfo](../apps/server/src/main/java/bhoon/sugang_helper/academic/crawling/dto/result/CrawlTargetInfo.java)
- [LoadedFile](../apps/server/src/main/java/bhoon/sugang_helper/common/dto/result/LoadedFile.java)
- [AuthenticatedUser](../apps/server/src/main/java/bhoon/sugang_helper/identity/user/dto/result/AuthenticatedUser.java)
- [NotificationDevice](../apps/server/src/main/java/bhoon/sugang_helper/identity/user/dto/result/NotificationDevice.java)
- [NotificationRecipient](../apps/server/src/main/java/bhoon/sugang_helper/identity/user/dto/result/NotificationRecipient.java)
- [UserAccountStatus](../apps/server/src/main/java/bhoon/sugang_helper/identity/user/dto/result/UserAccountStatus.java)
- [UserDashboardProfile](../apps/server/src/main/java/bhoon/sugang_helper/identity/user/dto/result/UserDashboardProfile.java)
- [UserDeviceResult](../apps/server/src/main/java/bhoon/sugang_helper/identity/user/dto/result/UserDeviceResult.java)
- [UserDisplayName](../apps/server/src/main/java/bhoon/sugang_helper/identity/user/dto/result/UserDisplayName.java)
- [UserIdentity](../apps/server/src/main/java/bhoon/sugang_helper/identity/user/dto/result/UserIdentity.java)
- [UserOnboardingResult](../apps/server/src/main/java/bhoon/sugang_helper/identity/user/dto/result/UserOnboardingResult.java)
- [UserProfileResult](../apps/server/src/main/java/bhoon/sugang_helper/identity/user/dto/result/UserProfileResult.java)
- [UserProfileUpdateResult](../apps/server/src/main/java/bhoon/sugang_helper/identity/user/dto/result/UserProfileUpdateResult.java)
- [UserSettingsResult](../apps/server/src/main/java/bhoon/sugang_helper/identity/user/dto/result/UserSettingsResult.java)
- [PrimaryTimetableSnapshot](../apps/server/src/main/java/bhoon/sugang_helper/planning/timetable/dto/result/PrimaryTimetableSnapshot.java)
- [NotificationHistoryItem](../apps/server/src/main/java/bhoon/sugang_helper/seatalert/notification/dto/result/NotificationHistoryItem.java)
- [SeatNotificationDeliveryClaim](../apps/server/src/main/java/bhoon/sugang_helper/seatalert/notification/dto/result/SeatNotificationDeliveryClaim.java)
- [AdminFeedbackAttachmentAccess](../apps/server/src/main/java/bhoon/sugang_helper/support/feedback/dto/result/AdminFeedbackAttachmentAccess.java)
- [AdminFeedbackAttachmentSummary](../apps/server/src/main/java/bhoon/sugang_helper/support/feedback/dto/result/AdminFeedbackAttachmentSummary.java)
- [AdminFeedbackDetail](../apps/server/src/main/java/bhoon/sugang_helper/support/feedback/dto/result/AdminFeedbackDetail.java)
- [AdminFeedbackReplySummary](../apps/server/src/main/java/bhoon/sugang_helper/support/feedback/dto/result/AdminFeedbackReplySummary.java)
- [AdminFeedbackSummary](../apps/server/src/main/java/bhoon/sugang_helper/support/feedback/dto/result/AdminFeedbackSummary.java)
- [FeedbackAttachmentDownload](../apps/server/src/main/java/bhoon/sugang_helper/support/feedback/dto/result/FeedbackAttachmentDownload.java)
