package bhoon.sugang_helper.support.feedback.service;

import bhoon.sugang_helper.audit.api.AuditRecorder;
import bhoon.sugang_helper.common.api.error.CustomException;
import bhoon.sugang_helper.common.api.error.ErrorCode;
import bhoon.sugang_helper.common.api.storage.FileStorage;
import bhoon.sugang_helper.common.api.web.PageableGuard;
import bhoon.sugang_helper.identity.api.notifications.NotificationRecipients;
import bhoon.sugang_helper.identity.api.users.CurrentUserQuery;
import bhoon.sugang_helper.identity.api.users.UserAccountQuery;
import bhoon.sugang_helper.identity.api.users.UserAccountStatus;
import bhoon.sugang_helper.identity.api.users.UserIdentity;
import bhoon.sugang_helper.seatalert.api.NotificationPublications;
import bhoon.sugang_helper.support.feedback.dto.request.FeedbackReplyCreateRequest;
import bhoon.sugang_helper.support.feedback.dto.request.FeedbackReplyUpdateRequest;
import bhoon.sugang_helper.support.feedback.dto.request.FeedbackStatusUpdateRequest;
import bhoon.sugang_helper.support.feedback.dto.response.AdminFeedbackAttachmentResponse;
import bhoon.sugang_helper.support.feedback.dto.response.AdminFeedbackDetailResponse;
import bhoon.sugang_helper.support.feedback.dto.response.AdminFeedbackReplyResponse;
import bhoon.sugang_helper.support.feedback.dto.response.AdminFeedbackResponse;
import bhoon.sugang_helper.support.feedback.dto.result.AdminFeedbackAttachmentAccess;
import bhoon.sugang_helper.support.feedback.dto.result.AdminFeedbackDetail;
import bhoon.sugang_helper.support.feedback.dto.result.AdminFeedbackSummary;
import bhoon.sugang_helper.support.feedback.dto.result.FeedbackAttachmentDownload;
import bhoon.sugang_helper.support.feedback.entity.Feedback;
import bhoon.sugang_helper.support.feedback.entity.FeedbackReply;
import bhoon.sugang_helper.support.feedback.enums.AdminFeedbackDeletionFilter;
import bhoon.sugang_helper.support.feedback.enums.AdminFeedbackOrder;
import bhoon.sugang_helper.support.feedback.enums.FeedbackStatus;
import bhoon.sugang_helper.support.feedback.repository.AdminFeedbackReadRepository;
import bhoon.sugang_helper.support.feedback.repository.FeedbackReplyRepository;
import bhoon.sugang_helper.support.feedback.repository.FeedbackRepository;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminFeedbackService {

    private final FeedbackRepository feedbackRepository;
    private final FeedbackReplyRepository feedbackReplyRepository;
    private final AuditRecorder adminAuditRecorder;
    private final AdminFeedbackReadRepository adminFeedbackReadRepository;
    private final CurrentUserQuery currentUserQuery;
    private final UserAccountQuery userAccountQuery;
    private final FileStorage fileUploadService;
    private final NotificationPublications notificationPublications;
    private final NotificationRecipients notificationRecipients;

    @Transactional
    public FeedbackAttachmentDownload getAttachmentForCurrentAdmin(Long feedbackId, Long attachmentId,
                                                                   boolean confirmed) {
        UserIdentity admin = requireCurrentAdmin();
        if (!confirmed) {
            throw new CustomException(ErrorCode.INVALID_INPUT, "첨부파일 열람을 확인해주세요.");
        }

        AdminFeedbackAttachmentAccess attachment = adminFeedbackReadRepository
                .findAttachmentForAdmin(feedbackId, attachmentId)
                .orElseThrow(() -> new CustomException(ErrorCode.FEEDBACK_NOT_FOUND));
        var file = fileUploadService.loadFile(attachment.fileUrl());
        adminAuditRecorder.recordAttachmentAccess(admin.id(), attachmentId, feedbackId);
        return new FeedbackAttachmentDownload(file.resource(), attachment.originalName(), file.contentType());
    }

    @Transactional(readOnly = true)
    public Page<AdminFeedbackResponse> getFeedbacksForAdmin(Pageable pageable,
                                                             AdminFeedbackDeletionFilter deletionFilter,
                                                             AdminFeedbackOrder order) {
        Page<AdminFeedbackSummary> feedbacks = adminFeedbackReadRepository.findFeedbacks(
                deletionFilter, order, PageableGuard.requireBounded(pageable));
        Map<Long, Boolean> withdrawnByUserId = findAccountStatuses(feedbacks.getContent().stream()
                .map(AdminFeedbackSummary::authorId)
                .toList());
        return feedbacks.map(feedback -> toAdminFeedbackResponse(feedback, withdrawnByUserId));
    }

    private AdminFeedbackResponse toAdminFeedbackResponse(AdminFeedbackSummary feedback,
                                                          Map<Long, Boolean> withdrawnByUserId) {
        boolean withdrawnAuthor = isWithdrawnAuthor(feedback.authorId(), withdrawnByUserId);
        return AdminFeedbackResponse.from(feedback, authorLabel(withdrawnAuthor));
    }

    @Transactional(readOnly = true)
    public AdminFeedbackDetailResponse getFeedbackDetailForAdmin(Long feedbackId) {
        AdminFeedbackDetail feedback = adminFeedbackReadRepository
                .findFeedbackDetail(feedbackId)
                .orElseThrow(() -> new CustomException(ErrorCode.FEEDBACK_NOT_FOUND));
        Map<Long, Boolean> withdrawnByUserId = findAccountStatuses(Stream.ofNullable(feedback.authorId()).toList());
        return AdminFeedbackDetailResponse.from(
                feedback,
                authorLabel(isWithdrawnAuthor(feedback.authorId(), withdrawnByUserId)),
                adminFeedbackReadRepository.findAttachments(feedbackId).stream()
                        .map(attachment -> AdminFeedbackAttachmentResponse.of(attachment.id()))
                        .toList(),
                adminFeedbackReadRepository.findReplies(feedbackId).stream()
                        .map(reply -> AdminFeedbackReplyResponse.from(reply, "관리자"))
                        .toList());
    }

    @Transactional
    public Long createFeedbackReplyForCurrentAdmin(Long feedbackId, FeedbackReplyCreateRequest request) {
        Long adminId = requireCurrentAdmin().id();
        userAccountQuery.findById(adminId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        Feedback feedback = requireFeedbackWithOwnerLockedForUpdate(feedbackId);
        FeedbackReply reply = FeedbackReply.builder()
                .feedback(feedback)
                .adminId(adminId)
                .content(request.content())
                .build();

        FeedbackReply savedReply = feedbackReplyRepository.save(reply);
        notificationPublications.publishFeedbackReply(feedback.getUserId(), feedbackId, savedReply.getId());

        adminAuditRecorder.recordReplyCreated(adminId, savedReply.getId(), feedbackId, request.content());

        return savedReply.getId();
    }

    @Transactional
    public void updateFeedbackReplyForCurrentAdmin(Long replyId, FeedbackReplyUpdateRequest request) {
        Long adminId = requireCurrentAdmin().id();
        userAccountQuery.findById(adminId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        FeedbackReply reply = requireReplyForUpdate(replyId);

        String oldContent = reply.getContent();
        reply.updateContent(request.content());

        adminAuditRecorder.recordReplyUpdated(adminId, replyId, reply.getFeedback().getId(), oldContent,
                request.content());
    }

    @Transactional
    public void deleteFeedbackReplyForCurrentAdmin(Long replyId) {
        Long adminId = requireCurrentAdmin().id();
        userAccountQuery.findById(adminId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        FeedbackReply reply = requireReplyForUpdate(replyId);

        String content = reply.getContent();
        reply.delete();
        notificationPublications.hideFeedbackReply(replyId);

        adminAuditRecorder.recordReplyDeleted(adminId, replyId, reply.getFeedback().getId(), content);
    }

    @Transactional
    public void updateFeedbackStatusForCurrentAdmin(Long feedbackId, FeedbackStatusUpdateRequest request) {
        Long adminId = requireCurrentAdmin().id();
        userAccountQuery.findById(adminId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        Feedback feedback = requireFeedbackForUpdate(feedbackId);
        FeedbackStatus oldStatus = feedback.getStatus();
        feedback.updateStatus(request.status());

        adminAuditRecorder.recordStatusChange(adminId, feedbackId, oldStatus.name(), request.status().name());
    }

    private FeedbackReply requireReplyForUpdate(Long replyId) {
        Long feedbackId = feedbackReplyRepository.findFeedbackIdById(replyId)
                .orElseThrow(() -> new CustomException(ErrorCode.FEEDBACK_REPLY_NOT_FOUND));
        requireFeedbackForUpdate(feedbackId);
        return feedbackReplyRepository.findByIdForUpdate(replyId)
                .orElseThrow(() -> new CustomException(ErrorCode.FEEDBACK_REPLY_NOT_FOUND));
    }

    private Feedback requireFeedbackForUpdate(Long feedbackId) {
        return feedbackRepository.findByIdForUpdate(feedbackId)
                .orElseThrow(() -> new CustomException(ErrorCode.FEEDBACK_NOT_FOUND));
    }

    private Feedback requireFeedbackWithOwnerLockedForUpdate(Long feedbackId) {
        Long ownerId = feedbackRepository.findOwnerIdById(feedbackId)
                .orElseThrow(() -> new CustomException(ErrorCode.FEEDBACK_NOT_FOUND));
        if (notificationRecipients.findActiveByIdsForShare(List.of(ownerId)).isEmpty()) {
            throw new CustomException(ErrorCode.FEEDBACK_NOT_FOUND);
        }
        return requireFeedbackForUpdate(feedbackId);
    }

    private UserIdentity requireCurrentAdmin() {
        UserIdentity user = currentUserQuery.requireCurrentUser();
        if (!user.administrator()) {
            throw new CustomException(ErrorCode.FEEDBACK_UNAUTHORIZED);
        }
        return user;
    }

    private String authorLabel(boolean withdrawn) {
        if (withdrawn) {
            return "탈퇴 사용자";
        }
        return "사용자";
    }

    private Map<Long, Boolean> findAccountStatuses(List<Long> authorIds) {
        List<Long> userIds = authorIds.stream()
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        return userAccountQuery.findAccountStatuses(userIds).stream()
                .collect(Collectors.toMap(UserAccountStatus::userId, UserAccountStatus::withdrawn));
    }

    private boolean isWithdrawnAuthor(Long authorId, Map<Long, Boolean> withdrawnByUserId) {
        return authorId == null || !withdrawnByUserId.containsKey(authorId)
                || Boolean.TRUE.equals(withdrawnByUserId.get(authorId));
    }
}
