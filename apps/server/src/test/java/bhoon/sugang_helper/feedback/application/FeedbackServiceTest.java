package bhoon.sugang_helper.feedback.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import bhoon.sugang_helper.admin.application.AdminAuditService;
import bhoon.sugang_helper.common.error.CustomException;
import bhoon.sugang_helper.common.error.ErrorCode;
import bhoon.sugang_helper.common.util.LocalFileUploadService;
import bhoon.sugang_helper.common.redis.RedisService;
import bhoon.sugang_helper.feedback.domain.AdminFeedbackDeletionFilter;
import bhoon.sugang_helper.feedback.domain.AdminFeedbackReadRepository;
import bhoon.sugang_helper.feedback.domain.Feedback;
import bhoon.sugang_helper.feedback.domain.FeedbackAttachment;
import bhoon.sugang_helper.feedback.domain.FeedbackAttachmentRepository;
import bhoon.sugang_helper.feedback.domain.FeedbackReply;
import bhoon.sugang_helper.feedback.domain.FeedbackReplyRepository;
import bhoon.sugang_helper.feedback.domain.FeedbackRepository;
import bhoon.sugang_helper.feedback.domain.FeedbackStatus;
import bhoon.sugang_helper.feedback.domain.FeedbackType;
import bhoon.sugang_helper.user.domain.User;
import bhoon.sugang_helper.user.domain.Role;
import bhoon.sugang_helper.user.domain.UserRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.multipart.MultipartFile;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("PMD.AvoidDuplicateLiterals") // Feedback request fixtures intentionally reuse compact payload values.
class FeedbackServiceTest {

    @Mock
    private FeedbackRepository feedbackRepository;
    @Mock
    private FeedbackAttachmentRepository feedbackAttachmentRepository;
    @Mock
    private FeedbackReplyRepository feedbackReplyRepository;
    @Mock
    private AdminAuditService adminAuditService;
    @Mock
    private AdminFeedbackReadRepository adminFeedbackReadRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private LocalFileUploadService fileUploadService;
    @Mock
    private RedisService redisService;

    @InjectMocks
    private FeedbackService feedbackService;

    @BeforeEach
    void setUpRateLimits() {
        org.springframework.test.util.ReflectionTestUtils.setField(feedbackService, "userRequestsPerMinute", 2L);
        org.springframework.test.util.ReflectionTestUtils.setField(feedbackService, "ipRequestsPerMinute", 4L);
        org.springframework.test.util.ReflectionTestUtils.setField(feedbackService, "dailyQuota", 5L);
        org.springframework.test.util.ReflectionTestUtils.setField(feedbackService, "maximumUploadBytes", 10_485_760L);
    }

    @Test
    @DisplayName("사용자가 이미지를 포함하여 피드백을 등록한다.")
    void createFeedbackWithImages() {
        // given
        Long userId = 1L;
        User user = User.builder().id(userId).name("유저").build();
        FeedbackCreateRequest request = new FeedbackCreateRequest(FeedbackType.BUG, "제목", "내용", "meta");
        MultipartFile mockFile = mock(MultipartFile.class);
        List<MultipartFile> files = List.of(mockFile);

        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(feedbackRepository.save(any(Feedback.class))).willAnswer(invocation -> invocation.getArgument(0));
        given(fileUploadService.uploadImages(anyList())).willReturn(List.of("/uploads/img1.png"));
        given(mockFile.getOriginalFilename()).willReturn("test.png");

        // when
        feedbackService.createFeedback(userId, request, files);

        // then
        verify(feedbackRepository).save(any(Feedback.class));
        verify(fileUploadService).uploadImages(anyList());
        verify(feedbackAttachmentRepository).save(any());
    }

    @Test
    @DisplayName("요청 한도를 초과한 사용자는 429 예외를 받는다.")
    void createFeedback_rejectsRateLimitedUser() {
        Long userId = 1L;
        User user = User.builder().id(userId).name("유저").build();
        FeedbackCreateRequest request = new FeedbackCreateRequest(FeedbackType.BUG, "제목", "내용", "meta");
        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(redisService.increment(any(), any())).willReturn(3L);

        assertThatThrownBy(() -> feedbackService.createFeedback(userId, request, List.of(), "203.0.113.1"))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.TOO_MANY_REQUESTS);
    }

    @Test
    @DisplayName("요청 카운터가 만료된 새 윈도우에서는 다시 문의를 작성할 수 있다.")
    void createFeedback_allowsRequestInNewRateLimitWindow() {
        Long userId = 1L;
        User user = User.builder().id(userId).name("유저").build();
        FeedbackCreateRequest request = new FeedbackCreateRequest(FeedbackType.BUG, "제목", "내용", "meta");
        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(feedbackRepository.save(any(Feedback.class))).willAnswer(invocation -> invocation.getArgument(0));
        given(redisService.increment(any(), any())).willReturn(1L);

        feedbackService.createFeedback(userId, request, List.of(), "203.0.113.1");

        verify(redisService).increment("FEEDBACK:RATE:USER:1", java.time.Duration.ofMinutes(1));
        verify(redisService).increment("FEEDBACK:RATE:IP:203.0.113.1", java.time.Duration.ofMinutes(1));
        verify(redisService).increment("FEEDBACK:RATE:DAILY:1", java.time.Duration.ofDays(1));
    }

    @Test
    @DisplayName("첨부파일 총 용량이 제한을 넘으면 저장 전에 차단한다.")
    void createFeedback_rejectsOversizedAttachments() {
        Long userId = 1L;
        User user = User.builder().id(userId).name("유저").build();
        MultipartFile oversizedFile = mock(MultipartFile.class);
        FeedbackCreateRequest request = new FeedbackCreateRequest(FeedbackType.BUG, "제목", "내용", "meta");
        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(oversizedFile.getSize()).willReturn(10_485_761L);

        assertThatThrownBy(() -> feedbackService.createFeedback(userId, request, List.of(oversizedFile), "203.0.113.1"))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.MAX_FILE_UPLOAD_SIZE_EXCEEDED);
        verify(fileUploadService, never()).uploadImages(anyList());
    }

    @Test
    @DisplayName("타인의 피드백을 상세 조회하려고 하면 예외가 발생한다.")
    void getMyFeedbackDetailUnauthorized() {
        // given
        Long userId = 1L;
        Long otherUserId = 2L;
        Long feedbackId = 10L;
        User otherUser = User.builder().id(otherUserId).build();
        Feedback feedback = Feedback.builder().user(otherUser).build();

        given(feedbackRepository.findById(feedbackId)).willReturn(Optional.of(feedback));

        // when & then
        assertThatThrownBy(() -> feedbackService.getMyFeedbackDetail(userId, feedbackId))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining(ErrorCode.FEEDBACK_UNAUTHORIZED.getMessage());
    }

    @Test
    @DisplayName("작성자가 아닌 사용자는 첨부파일을 다운로드할 수 없다.")
    void getAttachment_rejectsNonOwner() {
        Long feedbackId = 10L;
        Long attachmentId = 20L;
        User owner = User.builder().id(2L).role(Role.USER).build();
        User requester = User.builder().id(1L).role(Role.USER).build();
        Feedback feedback = Feedback.builder().user(owner).build();
        FeedbackAttachment attachment = FeedbackAttachment.builder()
                .feedback(feedback)
                .fileUrl("/uploads/private.png")
                .originalName("private.png")
                .build();

        org.springframework.test.util.ReflectionTestUtils.setField(feedback, "id", feedbackId);
        org.springframework.test.util.ReflectionTestUtils.setField(attachment, "id", attachmentId);
        given(feedbackRepository.findById(feedbackId)).willReturn(Optional.of(feedback));
        given(feedbackAttachmentRepository.findByIdAndFeedbackId(attachmentId, feedbackId)).willReturn(Optional.of(attachment));

        assertThatThrownBy(() -> feedbackService.getAttachment(requester, feedbackId, attachmentId))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.FEEDBACK_UNAUTHORIZED);
        verify(fileUploadService, never()).loadFile(any());
        verify(adminAuditService, never()).recordAttachmentAccess(any(), any(), any());
    }

    @Test
    @DisplayName("작성자도 다른 피드백의 첨부파일 ID를 섞어 다운로드할 수 없다.")
    void getAttachment_rejectsCrossFeedbackAttachment() {
        Long feedbackId = 10L;
        Long attachmentId = 20L;
        User owner = User.builder().id(1L).role(Role.USER).build();
        Feedback requestedFeedback = Feedback.builder().user(owner).build();
        org.springframework.test.util.ReflectionTestUtils.setField(requestedFeedback, "id", feedbackId);
        given(feedbackRepository.findById(feedbackId)).willReturn(Optional.of(requestedFeedback));
        given(feedbackAttachmentRepository.findByIdAndFeedbackId(attachmentId, feedbackId)).willReturn(Optional.empty());

        assertThatThrownBy(() -> feedbackService.getAttachment(owner, feedbackId, attachmentId))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.FEEDBACK_NOT_FOUND);

        verify(fileUploadService, never()).loadFile(any());
        verify(adminAuditService, never()).recordAttachmentAccess(any(), any(), any());
    }

    @Test
    @DisplayName("관리자는 일반 첨부파일 경로로 우회 다운로드할 수 없다.")
    void getAttachment_rejectsAdminBypass() {
        Long feedbackId = 10L;
        Long attachmentId = 20L;
        User admin = User.builder().id(1L).role(Role.ADMIN).build();
        assertThatThrownBy(() -> feedbackService.getAttachment(admin, feedbackId, attachmentId))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.FEEDBACK_UNAUTHORIZED);
        verify(fileUploadService, never()).loadFile(any());
        verify(adminAuditService, never()).recordAttachmentAccess(any(), any(), any());
    }

    @Test
    @DisplayName("관리자는 명시 확인 후에만 전용 경로로 첨부파일을 열람하고 감사 로그를 남긴다.")
    void getAttachmentForAdmin_requiresConfirmationAndAuditsSuccessfulAccess() {
        Long feedbackId = 10L;
        Long attachmentId = 20L;
        User admin = User.builder().id(1L).role(Role.ADMIN).build();
        org.springframework.core.io.Resource resource = mock(org.springframework.core.io.Resource.class);
        AdminFeedbackReadRepository.FeedbackAttachmentAccess attachment =
                new AdminFeedbackReadRepository.FeedbackAttachmentAccess(
                        attachmentId, feedbackId, "/uploads/private.png", "private.png");
        given(adminFeedbackReadRepository.findAttachmentForAdmin(feedbackId, attachmentId))
                .willReturn(Optional.of(attachment));
        given(fileUploadService.loadFile("/uploads/private.png")).willReturn(resource);

        assertThatThrownBy(() -> feedbackService.getAttachmentForAdmin(admin, feedbackId, attachmentId, false))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_INPUT);
        verify(adminFeedbackReadRepository, never()).findAttachmentForAdmin(feedbackId, attachmentId);
        FeedbackAttachmentDownload download = feedbackService.getAttachmentForAdmin(admin, feedbackId, attachmentId, true);

        assertThat(download.resource()).isSameAs(resource);
        assertThat(download.originalName()).isEqualTo("private.png");
        verify(fileUploadService).loadFile("/uploads/private.png");
        verify(adminAuditService).recordAttachmentAccess(admin, attachmentId, feedbackId);
    }

    @Test
    @DisplayName("관리자 첨부파일 리소스 해석이 실패하면 접근 감사 로그를 남기지 않는다.")
    void getAttachmentForAdmin_doesNotAuditWhenResourceLoadFails() {
        Long feedbackId = 10L;
        Long attachmentId = 20L;
        User admin = User.builder().id(1L).role(Role.ADMIN).build();
        given(adminFeedbackReadRepository.findAttachmentForAdmin(feedbackId, attachmentId)).willReturn(Optional.of(
                new AdminFeedbackReadRepository.FeedbackAttachmentAccess(
                        attachmentId, feedbackId, "/uploads/missing.png", "missing.png")));
        given(fileUploadService.loadFile("/uploads/missing.png"))
                .willThrow(new IllegalStateException("file unavailable"));

        assertThatThrownBy(() -> feedbackService.getAttachmentForAdmin(admin, feedbackId, attachmentId, true))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("file unavailable");

        verify(adminAuditService, never()).recordAttachmentAccess(any(), any(), any());
    }

    @Test
    @DisplayName("피드백을 삭제해도 관리자 보존용 첨부파일은 제거하지 않는다.")
    void deleteFeedback_preservesAttachmentsForAdminAudit() {
        Long userId = 1L;
        Long feedbackId = 10L;
        User owner = User.builder().id(userId).role(Role.USER).build();
        Feedback feedback = Feedback.builder().user(owner).build();
        given(feedbackRepository.findById(feedbackId)).willReturn(Optional.of(feedback));

        feedbackService.deleteFeedback(userId, feedbackId);

        assertThat(feedback.getDeletedAt()).isNotNull();
        verify(feedbackAttachmentRepository, never()).findAllByFeedbackId(feedbackId);
        verify(fileUploadService, never()).deleteFilesAfterTransactionCommit(anyList());
    }

    @Test
    @DisplayName("관리자가 피드백 상태를 직접 변경하고 액션 로그를 남긴다.")
    void updateFeedbackStatusByAdmin() {
        // given
        Long adminId = 1L;
        Long feedbackId = 10L;
        User admin = User.builder().id(adminId).build();
        Feedback feedback = Feedback.builder().title("제목").build();
        FeedbackStatusUpdateRequest request = new FeedbackStatusUpdateRequest(FeedbackStatus.COMPLETED);

        given(userRepository.findById(adminId)).willReturn(Optional.of(admin));
        given(feedbackRepository.findById(feedbackId)).willReturn(Optional.of(feedback));

        // when
        feedbackService.updateFeedbackStatus(adminId, feedbackId, request);

        // then
        assertThat(feedback.getStatus()).isEqualTo(FeedbackStatus.COMPLETED);
        verify(adminAuditService).recordStatusChange(admin, feedbackId, FeedbackStatus.PENDING, FeedbackStatus.COMPLETED);
    }

    @Test
    @DisplayName("관리자 답변 생성·수정·삭제는 공통 감사 서비스에 안전한 요약을 기록한다.")
    void manageFeedbackReply_recordsSharedAuditEvents() {
        Long adminId = 1L;
        Long feedbackId = 10L;
        Long replyId = 20L;
        User admin = User.builder().id(adminId).build();
        Feedback feedback = Feedback.builder().title("제목").build();
        FeedbackReply reply = FeedbackReply.builder().feedback(feedback).admin(admin).content("기존 \"답변\"\n😀").build();
        FeedbackReplyCreateRequest createRequest = new FeedbackReplyCreateRequest("새 답변");
        FeedbackReplyUpdateRequest updateRequest = new FeedbackReplyUpdateRequest("수정 답변");
        org.springframework.test.util.ReflectionTestUtils.setField(feedback, "id", feedbackId);
        org.springframework.test.util.ReflectionTestUtils.setField(reply, "id", replyId);
        given(userRepository.findById(adminId)).willReturn(Optional.of(admin));
        given(feedbackRepository.findById(feedbackId)).willReturn(Optional.of(feedback));
        given(feedbackReplyRepository.save(any(FeedbackReply.class))).willReturn(reply);
        given(feedbackReplyRepository.findById(replyId)).willReturn(Optional.of(reply));

        feedbackService.createFeedbackReply(adminId, feedbackId, createRequest);
        feedbackService.updateFeedbackReply(adminId, replyId, updateRequest);
        feedbackService.deleteFeedbackReply(adminId, replyId);

        verify(adminAuditService).recordReplyCreated(admin, replyId, feedbackId, "새 답변");
        verify(adminAuditService).recordReplyUpdated(admin, replyId, feedbackId, "기존 \"답변\"\n😀", "수정 답변");
        verify(adminAuditService).recordReplyDeleted(admin, replyId, feedbackId, "수정 답변");
        assertThat(reply.getDeletedAt()).isNotNull();
        verify(feedbackReplyRepository, never()).delete(reply);
    }

    @Test
    @DisplayName("삭제된 피드백에 속한 답변은 관리자 변경 API로 수정하거나 삭제할 수 없다.")
    void manageFeedbackReply_rejectsDeletedParentFeedback() {
        Long adminId = 1L;
        Long feedbackId = 10L;
        Long replyId = 20L;
        User admin = User.builder().id(adminId).role(Role.ADMIN).build();
        Feedback feedback = Feedback.builder().title("삭제 문의").build();
        FeedbackReply reply = FeedbackReply.builder().feedback(feedback).admin(admin).content("보존 답변").build();
        org.springframework.test.util.ReflectionTestUtils.setField(feedback, "id", feedbackId);
        org.springframework.test.util.ReflectionTestUtils.setField(reply, "id", replyId);
        given(userRepository.findById(adminId)).willReturn(Optional.of(admin));
        given(feedbackReplyRepository.findById(replyId)).willReturn(Optional.of(reply));
        given(feedbackRepository.findById(feedbackId)).willReturn(Optional.empty());

        assertThatThrownBy(() -> feedbackService.updateFeedbackReply(
                adminId, replyId, new FeedbackReplyUpdateRequest("우회 수정")))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.FEEDBACK_NOT_FOUND);
        assertThatThrownBy(() -> feedbackService.deleteFeedbackReply(adminId, replyId))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.FEEDBACK_NOT_FOUND);

        assertThat(reply.getContent()).isEqualTo("보존 답변");
        assertThat(reply.getDeletedAt()).isNull();
        verify(feedbackRepository, times(2)).findById(feedbackId);
        verify(adminAuditService, never()).recordReplyUpdated(any(), any(), any(), any(), any());
        verify(adminAuditService, never()).recordReplyDeleted(any(), any(), any(), any());
    }

    @Test
    @DisplayName("관리자 목록·상세는 삭제 콘텐츠를 포함하되 작성자 식별자와 환경 메타데이터는 제외한다.")
    void getFeedbacksForAdmin_includesDeletedContentWithMaskedIdentity() {
        Long feedbackId = 10L;
        LocalDateTime deletedAt = LocalDateTime.of(2026, 7, 13, 6, 0);
        AdminFeedbackReadRepository.FeedbackSummary summary = new AdminFeedbackReadRepository.FeedbackSummary(
                feedbackId,
                FeedbackType.BUG,
                "삭제 문의",
                FeedbackStatus.PENDING,
                deletedAt.minusDays(1),
                true,
                true,
                deletedAt,
                true);
        AdminFeedbackReadRepository.FeedbackDetail detail = new AdminFeedbackReadRepository.FeedbackDetail(
                feedbackId,
                FeedbackType.BUG,
                "삭제 문의",
                "관리자가 보존하는 본문",
                FeedbackStatus.PENDING,
                deletedAt.minusDays(1),
                true,
                deletedAt,
                true);
        given(adminFeedbackReadRepository.findFeedbacks(AdminFeedbackDeletionFilter.ALL, PageRequest.of(0, 20)))
                .willReturn(new PageImpl<>(List.of(summary), PageRequest.of(0, 20), 1));
        given(adminFeedbackReadRepository.findFeedbackDetail(feedbackId)).willReturn(Optional.of(detail));
        given(adminFeedbackReadRepository.findReplies(feedbackId)).willReturn(List.of(
                new AdminFeedbackReadRepository.FeedbackReplySummary(
                        30L, "삭제된 답변", deletedAt, deletedAt, true, deletedAt)));
        given(adminFeedbackReadRepository.findAttachments(feedbackId)).willReturn(List.of(
                new AdminFeedbackReadRepository.FeedbackAttachmentSummary(40L)));

        Page<AdminFeedbackResponse> list = feedbackService.getFeedbacksForAdmin(
                PageRequest.of(0, 20), AdminFeedbackDeletionFilter.ALL);
        AdminFeedbackDetailResponse response = feedbackService.getFeedbackDetailForAdmin(feedbackId);

        assertThat(list.getContent()).singleElement().satisfies(item -> {
            assertThat(item.deleted()).isTrue();
            assertThat(item.authorLabel()).isEqualTo("탈퇴 사용자");
        });
        assertThat(response.deleted()).isTrue();
        assertThat(response.authorLabel()).isEqualTo("탈퇴 사용자");
        assertThat(response.content()).isEqualTo("관리자가 보존하는 본문");
        assertThat(response.attachments()).extracting(AdminFeedbackAttachmentResponse::id).containsExactly(40L);
        assertThat(response.toString())
                .doesNotContain("private.example")
                .doesNotContain("originalName")
                .doesNotContain("withdrawn@example.com");
    }

    @Test
    @DisplayName("활성 피드백도 관리자 응답에서는 환경 메타 정보를 제외한다.")
    void getFeedbackDetailForAdmin_excludesActiveMetaInfo() {
        Long feedbackId = 10L;
        AdminFeedbackReadRepository.FeedbackDetail detail = new AdminFeedbackReadRepository.FeedbackDetail(
                feedbackId,
                FeedbackType.BUG,
                "활성 문의",
                "본문",
                FeedbackStatus.PENDING,
                LocalDateTime.of(2026, 7, 13, 6, 0),
                false,
                null,
                false);
        given(adminFeedbackReadRepository.findFeedbackDetail(feedbackId)).willReturn(Optional.of(detail));
        given(adminFeedbackReadRepository.findAttachments(feedbackId)).willReturn(List.of());
        given(adminFeedbackReadRepository.findReplies(feedbackId)).willReturn(List.of());

        AdminFeedbackDetailResponse response = feedbackService.getFeedbackDetailForAdmin(feedbackId);

        assertThat(response.content()).isEqualTo("본문");
        assertThat(List.of(AdminFeedbackDetailResponse.class.getRecordComponents()))
                .extracting(component -> component.getName())
                .doesNotContain("metaInfo");
    }
}
