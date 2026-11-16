package bhoon.sugang_helper.support.feedback.service;

import bhoon.sugang_helper.common.api.cache.RedisAccess;
import bhoon.sugang_helper.common.api.error.CustomException;
import bhoon.sugang_helper.common.api.error.ErrorCode;
import bhoon.sugang_helper.common.api.storage.FileStorage;
import bhoon.sugang_helper.common.api.web.PageableGuard;
import bhoon.sugang_helper.identity.api.users.CurrentUserQuery;
import bhoon.sugang_helper.identity.api.users.UserAccountQuery;
import bhoon.sugang_helper.identity.api.users.UserDisplayName;
import bhoon.sugang_helper.identity.api.users.UserDisplayNames;
import bhoon.sugang_helper.identity.api.users.UserIdentity;
import bhoon.sugang_helper.seatalert.api.NotificationPublications;
import bhoon.sugang_helper.support.feedback.dto.request.FeedbackCreateRequest;
import bhoon.sugang_helper.support.feedback.dto.response.FeedbackDetailResponse;
import bhoon.sugang_helper.support.feedback.dto.response.FeedbackResponse;
import bhoon.sugang_helper.support.feedback.dto.result.FeedbackAttachmentDownload;
import bhoon.sugang_helper.support.feedback.entity.Feedback;
import bhoon.sugang_helper.support.feedback.entity.FeedbackAttachment;
import bhoon.sugang_helper.support.feedback.entity.FeedbackReply;
import bhoon.sugang_helper.support.feedback.repository.FeedbackAttachmentRepository;
import bhoon.sugang_helper.support.feedback.repository.FeedbackRepository;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Service
@RequiredArgsConstructor
public class FeedbackService {

    private static final String USER_RATE_LIMIT_KEY_PREFIX = "FEEDBACK:RATE:USER:";
    private static final String IP_RATE_LIMIT_KEY_PREFIX = "FEEDBACK:RATE:IP:";
    private static final String DAILY_QUOTA_KEY_PREFIX = "FEEDBACK:RATE:DAILY:";
    private static final Duration RATE_LIMIT_WINDOW = Duration.ofMinutes(1);
    private static final Duration DAILY_QUOTA_WINDOW = Duration.ofDays(1);

    private final FeedbackRepository feedbackRepository;
    private final FeedbackAttachmentRepository feedbackAttachmentRepository;
    private final CurrentUserQuery currentUserQuery;
    private final UserAccountQuery userAccountQuery;
    private final UserDisplayNames userDisplayNameQuery;
    private final FileStorage fileUploadService;
    private final RedisAccess redisService;
    private final FeedbackMetadataNormalizer feedbackMetadataNormalizer;
    private final NotificationPublications notificationPublications;

    @Value("${app.feedback.rate-limit.user-per-minute:5}")
    private long userRequestsPerMinute;

    @Value("${app.feedback.rate-limit.ip-per-minute:10}")
    private long ipRequestsPerMinute;

    @Value("${app.feedback.rate-limit.daily-quota:5}")
    private long dailyQuota;

    @Value("${app.feedback.max-upload-bytes:10485760}")
    private long maximumUploadBytes;

    @Transactional
    public Long createFeedbackForCurrentUser(FeedbackCreateRequest request, List<MultipartFile> files, String clientIp) {
        Long userId = currentUserQuery.requireCurrentUser().id();
        String normalizedMetaInfo = feedbackMetadataNormalizer.normalize(request.metaInfo());
        userAccountQuery.findById(userId)
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

        saveAttachments(savedFeedback, files);

        return savedFeedback.getId();
    }

    private void saveAttachments(Feedback feedback, List<MultipartFile> files) {
        if (files == null || files.isEmpty()) {
            return;
        }
        List<String> fileUrls = fileUploadService.uploadImages(files);
        fileUploadService.deleteFilesAfterTransactionRollback(fileUrls);

        for (int i = 0; i < fileUrls.size(); i++) {
            FeedbackAttachment attachment = FeedbackAttachment.builder()
                    .feedback(feedback)
                    .fileUrl(fileUrls.get(i))
                    .originalName(sanitizedAttachmentName(files.get(i).getOriginalFilename(), fileUrls.get(i)))
                    .build();
            feedbackAttachmentRepository.save(attachment);
        }
    }

    private String sanitizedAttachmentName(String originalName, String fileUrl) {
        String baseName = originalName;
        if (!StringUtils.hasText(baseName)) {
            baseName = "unknown";
        }
        int originalExtensionStart = baseName.lastIndexOf('.');
        if (originalExtensionStart > 0) {
            baseName = baseName.substring(0, originalExtensionStart);
        }
        int sanitizedExtensionStart = fileUrl.lastIndexOf('.');
        return baseName + fileUrl.substring(sanitizedExtensionStart);
    }

    @Transactional(readOnly = true)
    public Page<FeedbackResponse> getMyFeedbacksForCurrentUser(Pageable pageable) {
        Long userId = currentUserQuery.requireCurrentUser().id();
        return feedbackRepository.findListItemsByUserId(userId, PageableGuard.requireBounded(pageable))
                .map(FeedbackResponse::from);
    }

    @Transactional(readOnly = true)
    public FeedbackDetailResponse getMyFeedbackDetailForCurrentUser(Long feedbackId) {
        Long userId = currentUserQuery.requireCurrentUser().id();
        Feedback feedback = feedbackRepository.findById(feedbackId)
                .orElseThrow(() -> new CustomException(ErrorCode.FEEDBACK_NOT_FOUND));
        requireOwner(feedback, userId);
        return FeedbackDetailResponse.from(feedback, adminNamesById(feedback.getReplies()));
    }

    @Transactional
    public void deleteCurrentUsersFeedback(Long feedbackId) {
        Long userId = currentUserQuery.requireCurrentUser().id();
        Feedback feedback = feedbackRepository.findByIdForUpdate(feedbackId)
                .orElseThrow(() -> new CustomException(ErrorCode.FEEDBACK_NOT_FOUND));
        requireOwner(feedback, userId);
        feedback.delete();
        notificationPublications.hideFeedbackReplies(feedbackId);
    }

    @Transactional
    public FeedbackAttachmentDownload getAttachmentForCurrentUser(Long feedbackId, Long attachmentId) {
        UserIdentity requester = currentUserQuery.requireCurrentUser();
        if (requester.administrator()) {
            throw new CustomException(ErrorCode.FEEDBACK_UNAUTHORIZED);
        }

        FeedbackAttachment attachment = feedbackAttachmentRepository.findByIdAndFeedbackId(attachmentId, feedbackId)
                .orElseThrow(() -> new CustomException(ErrorCode.FEEDBACK_NOT_FOUND));
        if (!requester.id().equals(attachment.getFeedback().getUserId())) {
            throw new CustomException(ErrorCode.FEEDBACK_UNAUTHORIZED);
        }
        var file = fileUploadService.loadFile(attachment.getFileUrl());
        return new FeedbackAttachmentDownload(file.resource(), attachment.getOriginalName(), file.contentType());
    }

    private void requireOwner(Feedback feedback, Long userId) {
        if (!feedback.getUserId().equals(userId)) {
            throw new CustomException(ErrorCode.FEEDBACK_UNAUTHORIZED);
        }
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
        long totalUploadBytes = 0;
        if (files != null) {
            totalUploadBytes = files.stream().mapToLong(MultipartFile::getSize).sum();
        }
        if (maximumUploadBytes > 0 && totalUploadBytes > maximumUploadBytes) {
            throw new CustomException(ErrorCode.MAX_FILE_UPLOAD_SIZE_EXCEEDED);
        }

        requireWithinLimit(USER_RATE_LIMIT_KEY_PREFIX + userId, userRequestsPerMinute, RATE_LIMIT_WINDOW);
        if (StringUtils.hasText(clientIp)) {
            requireWithinLimit(IP_RATE_LIMIT_KEY_PREFIX + clientIp.trim(), ipRequestsPerMinute, RATE_LIMIT_WINDOW);
        }

        if (dailyQuota > 0 && redisService.increment(DAILY_QUOTA_KEY_PREFIX + userId, DAILY_QUOTA_WINDOW) > dailyQuota) {
            throw new CustomException(ErrorCode.DAILY_FEEDBACK_LIMIT_EXCEEDED);
        }
    }

    private void requireWithinLimit(String key, long limit, Duration duration) {
        if (limit > 0 && redisService.increment(key, duration) > limit) {
            throw new CustomException(ErrorCode.TOO_MANY_REQUESTS);
        }
    }
}
