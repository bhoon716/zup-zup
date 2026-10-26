package bhoon.sugang_helper.support.feedback.service;

import bhoon.sugang_helper.admin.audit.service.AdminAuditService;
import bhoon.sugang_helper.common.error.CustomException;
import bhoon.sugang_helper.common.error.ErrorCode;
import bhoon.sugang_helper.common.redis.RedisService;
import bhoon.sugang_helper.common.util.LocalFileUploadService;
import bhoon.sugang_helper.common.util.SecurityUtil;
import bhoon.sugang_helper.common.web.PageableGuard;
import bhoon.sugang_helper.identity.user.dto.result.UserAccountStatus;
import bhoon.sugang_helper.identity.user.dto.result.UserDisplayName;
import bhoon.sugang_helper.identity.user.dto.result.UserIdentity;
import bhoon.sugang_helper.identity.user.service.UserDisplayNameQueryService;
import bhoon.sugang_helper.identity.user.service.UserIdentityQueryService;
import bhoon.sugang_helper.support.feedback.dto.result.AdminFeedbackAttachmentAccess;
import bhoon.sugang_helper.support.feedback.dto.result.AdminFeedbackDetail;
import bhoon.sugang_helper.support.feedback.dto.result.AdminFeedbackSummary;
import bhoon.sugang_helper.support.feedback.dto.result.FeedbackAttachmentDownload;
import bhoon.sugang_helper.support.feedback.dto.request.FeedbackCreateRequest;
import bhoon.sugang_helper.support.feedback.dto.request.FeedbackReplyCreateRequest;
import bhoon.sugang_helper.support.feedback.dto.request.FeedbackReplyUpdateRequest;
import bhoon.sugang_helper.support.feedback.dto.request.FeedbackStatusUpdateRequest;
import bhoon.sugang_helper.support.feedback.dto.response.AdminFeedbackAttachmentResponse;
import bhoon.sugang_helper.support.feedback.dto.response.AdminFeedbackDetailResponse;
import bhoon.sugang_helper.support.feedback.dto.response.AdminFeedbackReplyResponse;
import bhoon.sugang_helper.support.feedback.dto.response.AdminFeedbackResponse;
import bhoon.sugang_helper.support.feedback.dto.response.FeedbackDetailResponse;
import bhoon.sugang_helper.support.feedback.dto.response.FeedbackResponse;
import bhoon.sugang_helper.support.feedback.entity.Feedback;
import bhoon.sugang_helper.support.feedback.entity.FeedbackAttachment;
import bhoon.sugang_helper.support.feedback.entity.FeedbackReply;
import bhoon.sugang_helper.support.feedback.enums.AdminFeedbackDeletionFilter;
import bhoon.sugang_helper.support.feedback.enums.FeedbackStatus;
import bhoon.sugang_helper.support.feedback.repository.AdminFeedbackReadRepository;
import bhoon.sugang_helper.support.feedback.repository.FeedbackAttachmentRepository;
import bhoon.sugang_helper.support.feedback.repository.FeedbackReplyRepository;
import bhoon.sugang_helper.support.feedback.repository.FeedbackRepository;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;




@Slf4j
@Service
@RequiredArgsConstructor
public class FeedbackService {

    private final FeedbackRepository feedbackRepository;
    private final FeedbackAttachmentRepository feedbackAttachmentRepository;
    private final FeedbackReplyRepository feedbackReplyRepository;
    private final AdminAuditService adminAuditRecorder;
    private final AdminFeedbackReadRepository adminFeedbackReadRepository;
    private final UserIdentityQueryService userIdentityQuery;
    private final UserDisplayNameQueryService userDisplayNameQuery;
    private final LocalFileUploadService fileUploadService;
    private final RedisService redisService;
    private final FeedbackMetadataNormalizer feedbackMetadataNormalizer;
    @Value("${app.feedback.rate-limit.user-per-minute:5}")
    private long userRequestsPerMinute;
    @Value("${app.feedback.rate-limit.ip-per-minute:10}")
    private long ipRequestsPerMinute;
    @Value("${app.feedback.rate-limit.daily-quota:5}")
    private long dailyQuota;
    @Value("${app.feedback.max-upload-bytes:10485760}")
    private long maximumUploadBytes;




    @Transactional
    public Long createFeedback(Long userId, FeedbackCreateRequest request, List<MultipartFile> files) {
        return createFeedback(userId, request, files, null);
    }

    @Transactional
    public Long createFeedbackForCurrentUser(FeedbackCreateRequest request, List<MultipartFile> files, String clientIp) {
        return createFeedback(requireCurrentUser().id(), request, files, clientIp);
    }

    @Transactional
    public Long createFeedback(Long userId, FeedbackCreateRequest request, List<MultipartFile> files, String clientIp) {
        String normalizedMetaInfo = feedbackMetadataNormalizer.normalize(request.metaInfo());
        userIdentityQuery.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        validateRateLimit(userId, clientIp, files);

        Feedback feedback = Feedback.builder()
                .userId(userId)
                .type(request.type())
                .title(request.title())
                .content(request.content())
                .metaInfo(normalizedMetaInfo)
                .build();

        Feedback savedFeedback = feedbackRepository.save(feedback);

        if (files != null && !files.isEmpty()) {
            List<String> fileUrls = fileUploadService.uploadImages(files);
            fileUploadService.deleteFilesAfterTransactionRollback(fileUrls);

            for (int i = 0; i < fileUrls.size(); i++) {
                FeedbackAttachment attachment = FeedbackAttachment.builder()
                        .feedback(savedFeedback)
                        .fileUrl(fileUrls.get(i))
                        .originalName(sanitizedAttachmentName(files.get(i).getOriginalFilename(), fileUrls.get(i)))
                        .build();
                feedbackAttachmentRepository.save(attachment);
            }
        }

        return savedFeedback.getId();
    }

    private String sanitizedAttachmentName(String originalName, String fileUrl) {
        String name = originalName == null || originalName.isBlank() ? "unknown" : originalName;
        int originalExtensionStart = name.lastIndexOf('.');
        String baseName = originalExtensionStart > 0 ? name.substring(0, originalExtensionStart) : name;
        int sanitizedExtensionStart = fileUrl.lastIndexOf('.');
        return baseName + fileUrl.substring(sanitizedExtensionStart);
    }




    @Transactional(readOnly = true)
    public Page<FeedbackResponse> getMyFeedbacks(Long userId, Pageable pageable) {
        return feedbackRepository.findAllByUserId(userId, PageableGuard.requireBounded(pageable))
                .map(FeedbackResponse::from);
    }

    @Transactional(readOnly = true)
    public Page<FeedbackResponse> getMyFeedbacksForCurrentUser(Pageable pageable) {
        return getMyFeedbacks(requireCurrentUser().id(), pageable);
    }




    @Transactional(readOnly = true)
    public FeedbackDetailResponse getMyFeedbackDetail(Long userId, Long feedbackId) {
        Feedback feedback = feedbackRepository.findById(feedbackId)
                .orElseThrow(() -> new CustomException(ErrorCode.FEEDBACK_NOT_FOUND));

        if (!feedback.getUserId().equals(userId)) {
            throw new CustomException(ErrorCode.FEEDBACK_UNAUTHORIZED);
        }

        return FeedbackDetailResponse.from(feedback, adminNamesById(feedback.getReplies()));
    }

    @Transactional(readOnly = true)
    public FeedbackDetailResponse getMyFeedbackDetailForCurrentUser(Long feedbackId) {
        return getMyFeedbackDetail(requireCurrentUser().id(), feedbackId);
    }




    @Transactional
    public void deleteFeedback(Long userId, Long feedbackId) {
        Feedback feedback = feedbackRepository.findById(feedbackId)
                .orElseThrow(() -> new CustomException(ErrorCode.FEEDBACK_NOT_FOUND));

        if (!feedback.getUserId().equals(userId)) {
            throw new CustomException(ErrorCode.FEEDBACK_UNAUTHORIZED);
        }

        feedback.delete();
    }

    @Transactional
    public void deleteCurrentUsersFeedback(Long feedbackId) {
        deleteFeedback(requireCurrentUser().id(), feedbackId);
    }

    @Transactional
    public FeedbackAttachmentDownload getAttachment(UserIdentity requester, Long feedbackId, Long attachmentId) {
        if (requester.administrator()) {
            throw new CustomException(ErrorCode.FEEDBACK_UNAUTHORIZED);
        }

        Feedback feedback = feedbackRepository.findById(feedbackId)
                .orElseThrow(() -> new CustomException(ErrorCode.FEEDBACK_NOT_FOUND));
        FeedbackAttachment attachment = feedbackAttachmentRepository.findByIdAndFeedbackId(attachmentId, feedbackId)
                .orElseThrow(() -> new CustomException(ErrorCode.FEEDBACK_NOT_FOUND));
        if (!requester.id().equals(feedback.getUserId())) {
            throw new CustomException(ErrorCode.FEEDBACK_UNAUTHORIZED);
        }
        var file = fileUploadService.loadFile(attachment.getFileUrl());
        return new FeedbackAttachmentDownload(file.resource(), attachment.getOriginalName(), file.contentType());
    }

    @Transactional
    public FeedbackAttachmentDownload getAttachmentForCurrentUser(Long feedbackId, Long attachmentId) {
        return getAttachment(requireCurrentUser(), feedbackId, attachmentId);
    }




    @Transactional
    public FeedbackAttachmentDownload getAttachmentForAdmin(UserIdentity admin, Long feedbackId, Long attachmentId,
                                                            boolean confirmed) {
        if (!admin.administrator()) {
            throw new CustomException(ErrorCode.FEEDBACK_UNAUTHORIZED);
        }
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

    @Transactional
    public FeedbackAttachmentDownload getAttachmentForCurrentAdmin(Long feedbackId, Long attachmentId,
                                                                   boolean confirmed) {
        return getAttachmentForAdmin(requireCurrentAdmin(), feedbackId, attachmentId, confirmed);
    }






    @Transactional(readOnly = true)
    public Page<AdminFeedbackResponse> getFeedbacksForAdmin(Pageable pageable,
                                                             AdminFeedbackDeletionFilter deletionFilter) {
        Page<AdminFeedbackSummary> feedbacks = adminFeedbackReadRepository.findFeedbacks(
                deletionFilter, PageableGuard.requireBounded(pageable));
        Map<Long, Boolean> withdrawnByUserId = findAccountStatuses(feedbacks.getContent().stream()
                .map(AdminFeedbackSummary::authorId)
                .toList());
        return feedbacks
                .map(feedback -> new AdminFeedbackResponse(
                        feedback.id(),
                        feedback.type(),
                        feedback.title(),
                        feedback.status(),
                        feedback.createdAt(),
                        feedback.hasReplies(),
                        feedback.deleted(),
                        feedback.deletedAt(),
                        authorLabel(isWithdrawnAuthor(feedback.authorId(), withdrawnByUserId))));
    }




    @Transactional(readOnly = true)
    public AdminFeedbackDetailResponse getFeedbackDetailForAdmin(Long feedbackId) {
        AdminFeedbackDetail feedback = adminFeedbackReadRepository
                .findFeedbackDetail(feedbackId)
                .orElseThrow(() -> new CustomException(ErrorCode.FEEDBACK_NOT_FOUND));
        Map<Long, Boolean> withdrawnByUserId = findAccountStatuses(feedback.authorId() == null
                ? List.of()
                : List.of(feedback.authorId()));
        return new AdminFeedbackDetailResponse(
                feedback.id(),
                feedback.type(),
                feedback.title(),
                feedback.content(),
                feedback.status(),
                feedback.createdAt(),
                feedback.deleted(),
                feedback.deletedAt(),
                authorLabel(isWithdrawnAuthor(feedback.authorId(), withdrawnByUserId)),
                adminFeedbackReadRepository.findAttachments(feedbackId).stream()
                        .map(attachment -> new AdminFeedbackAttachmentResponse(attachment.id()))
                        .toList(),
                adminFeedbackReadRepository.findReplies(feedbackId).stream()
                        .map(reply -> new AdminFeedbackReplyResponse(
                                reply.id(),
                                "관리자",
                                reply.content(),
                                reply.createdAt(),
                                reply.updatedAt(),
                                reply.deleted(),
                                reply.deletedAt()))
                        .toList());
    }




    @Transactional
    public Long createFeedbackReply(Long adminId, Long feedbackId, FeedbackReplyCreateRequest request) {
        userIdentityQuery.findById(adminId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        Feedback feedback = feedbackRepository.findById(feedbackId)
                .orElseThrow(() -> new CustomException(ErrorCode.FEEDBACK_NOT_FOUND));

        FeedbackReply reply = FeedbackReply.builder()
                .feedback(feedback)
                .adminId(adminId)
                .content(request.content())
                .build();

        FeedbackReply savedReply = feedbackReplyRepository.save(reply);

        adminAuditRecorder.recordReplyCreated(adminId, savedReply.getId(), feedbackId, request.content());

        return savedReply.getId();
    }

    @Transactional
    public Long createFeedbackReplyForCurrentAdmin(Long feedbackId, FeedbackReplyCreateRequest request) {
        return createFeedbackReply(requireCurrentAdmin().id(), feedbackId, request);
    }




    @Transactional
    public void updateFeedbackReply(Long adminId, Long replyId, FeedbackReplyUpdateRequest request) {
        userIdentityQuery.findById(adminId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        FeedbackReply reply = feedbackReplyRepository.findById(replyId)
                .orElseThrow(() -> new CustomException(ErrorCode.FEEDBACK_REPLY_NOT_FOUND));
        Feedback feedback = feedbackRepository.findById(reply.getFeedback().getId())
                .orElseThrow(() -> new CustomException(ErrorCode.FEEDBACK_NOT_FOUND));

        String oldContent = reply.getContent();
        reply.updateContent(request.content());

        adminAuditRecorder.recordReplyUpdated(adminId, reply.getId(), feedback.getId(), oldContent,
                request.content());
    }

    @Transactional
    public void updateFeedbackReplyForCurrentAdmin(Long replyId, FeedbackReplyUpdateRequest request) {
        updateFeedbackReply(requireCurrentAdmin().id(), replyId, request);
    }




    @Transactional
    public void deleteFeedbackReply(Long adminId, Long replyId) {
        userIdentityQuery.findById(adminId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        FeedbackReply reply = feedbackReplyRepository.findById(replyId)
                .orElseThrow(() -> new CustomException(ErrorCode.FEEDBACK_REPLY_NOT_FOUND));
        Feedback feedback = feedbackRepository.findById(reply.getFeedback().getId())
                .orElseThrow(() -> new CustomException(ErrorCode.FEEDBACK_NOT_FOUND));

        String content = reply.getContent();
        reply.delete();

        adminAuditRecorder.recordReplyDeleted(adminId, replyId, feedback.getId(), content);
    }

    @Transactional
    public void deleteFeedbackReplyForCurrentAdmin(Long replyId) {
        deleteFeedbackReply(requireCurrentAdmin().id(), replyId);
    }




    @Transactional
    public void updateFeedbackStatus(Long adminId, Long feedbackId, FeedbackStatusUpdateRequest request) {
        userIdentityQuery.findById(adminId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        Feedback feedback = feedbackRepository.findById(feedbackId)
                .orElseThrow(() -> new CustomException(ErrorCode.FEEDBACK_NOT_FOUND));

        FeedbackStatus oldStatus = feedback.getStatus();
        feedback.updateStatus(request.status());

        adminAuditRecorder.recordStatusChange(adminId, feedbackId, oldStatus.name(), request.status().name());
    }

    @Transactional
    public void updateFeedbackStatusForCurrentAdmin(Long feedbackId, FeedbackStatusUpdateRequest request) {
        updateFeedbackStatus(requireCurrentAdmin().id(), feedbackId, request);
    }

    private UserIdentity requireCurrentUser() {
        String email = SecurityUtil.getCurrentUserEmail();
        return userIdentityQuery.findByEmail(email)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_UNAUTHORIZED));
    }

    private UserIdentity requireCurrentAdmin() {
        UserIdentity user = requireCurrentUser();
        if (!user.administrator()) {
            throw new CustomException(ErrorCode.FEEDBACK_UNAUTHORIZED);
        }
        return user;
    }

    private Map<Long, String> adminNamesById(List<FeedbackReply> replies) {
        List<Long> adminIds = replies.stream()
                .map(FeedbackReply::getAdminId)
                .distinct()
                .toList();
        if (adminIds.isEmpty()) {
            return Map.of();
        }
        return userDisplayNameQuery.findByIds(adminIds).stream()
                .collect(Collectors.toMap(UserDisplayName::id, UserDisplayName::name, (first, ignored) -> first));
    }




    private void validateRateLimit(Long userId, String clientIp, List<MultipartFile> files) {
        long totalUploadBytes = files == null ? 0 : files.stream().mapToLong(MultipartFile::getSize).sum();
        if (maximumUploadBytes > 0 && totalUploadBytes > maximumUploadBytes) {
            throw new CustomException(ErrorCode.MAX_FILE_UPLOAD_SIZE_EXCEEDED);
        }

        requireWithinLimit("FEEDBACK:RATE:USER:" + userId, userRequestsPerMinute, Duration.ofMinutes(1));
        if (clientIp != null && !clientIp.isBlank()) {
            requireWithinLimit("FEEDBACK:RATE:IP:" + clientIp.trim(), ipRequestsPerMinute, Duration.ofMinutes(1));
        }

        if (dailyQuota > 0 && redisService.increment("FEEDBACK:RATE:DAILY:" + userId, Duration.ofDays(1)) > dailyQuota) {
            throw new CustomException(ErrorCode.DAILY_FEEDBACK_LIMIT_EXCEEDED);
        }
    }

    private void requireWithinLimit(String key, long limit, Duration duration) {
        if (limit > 0 && redisService.increment(key, duration) > limit) {
            throw new CustomException(ErrorCode.TOO_MANY_REQUESTS);
        }
    }

    private String authorLabel(boolean withdrawn) {
        return withdrawn ? "탈퇴 사용자" : "사용자";
    }

    private Map<Long, Boolean> findAccountStatuses(List<Long> authorIds) {
        List<Long> userIds = authorIds.stream()
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        return userIdentityQuery.findAccountStatuses(userIds).stream()
                .collect(Collectors.toMap(UserAccountStatus::userId, UserAccountStatus::withdrawn));
    }

    private boolean isWithdrawnAuthor(Long authorId, Map<Long, Boolean> withdrawnByUserId) {
        return authorId == null || !withdrawnByUserId.containsKey(authorId)
                || Boolean.TRUE.equals(withdrawnByUserId.get(authorId));
    }
}
