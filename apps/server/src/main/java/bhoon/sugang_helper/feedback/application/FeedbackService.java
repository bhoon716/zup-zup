package bhoon.sugang_helper.feedback.application;

import bhoon.sugang_helper.admin.application.AdminAuditService;
import bhoon.sugang_helper.common.error.CustomException;
import bhoon.sugang_helper.common.error.ErrorCode;
import bhoon.sugang_helper.common.redis.RedisService;
import bhoon.sugang_helper.common.util.LocalFileUploadService;
import bhoon.sugang_helper.feedback.domain.Feedback;
import bhoon.sugang_helper.feedback.domain.AdminFeedbackDeletionFilter;
import bhoon.sugang_helper.feedback.domain.AdminFeedbackReadRepository;
import bhoon.sugang_helper.feedback.domain.FeedbackAttachment;
import bhoon.sugang_helper.feedback.domain.FeedbackAttachmentRepository;
import bhoon.sugang_helper.feedback.domain.FeedbackReply;
import bhoon.sugang_helper.feedback.domain.FeedbackReplyRepository;
import bhoon.sugang_helper.feedback.domain.FeedbackRepository;
import bhoon.sugang_helper.feedback.domain.FeedbackStatus;
import bhoon.sugang_helper.user.domain.Role;
import bhoon.sugang_helper.user.domain.User;
import bhoon.sugang_helper.user.domain.UserRepository;
import java.util.List;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/**
 * 문의 및 건의사항(버그 리포트, 기능 제안 등) 관련 비즈니스 로직을 처리하는 서비스 클래스입니다. 사용자의 의견 등록, 조회 및 관리자의 답변 관리 기능을 담당합니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FeedbackService {

    private final FeedbackRepository feedbackRepository;
    private final FeedbackAttachmentRepository feedbackAttachmentRepository;
    private final FeedbackReplyRepository feedbackReplyRepository;
    private final AdminAuditService adminAuditService;
    private final AdminFeedbackReadRepository adminFeedbackReadRepository;
    private final UserRepository userRepository;
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

    /**
     * 사용자의 새로운 문의 및 건의사항을 등록하고 첨부 파일을 저장합니다.
     */
    @Transactional
    public Long createFeedback(Long userId, FeedbackCreateRequest request, List<MultipartFile> files) {
        return createFeedback(userId, request, files, null);
    }

    @Transactional
    public Long createFeedback(Long userId, FeedbackCreateRequest request, List<MultipartFile> files, String clientIp) {
        String normalizedMetaInfo = feedbackMetadataNormalizer.normalize(request.metaInfo());
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        validateRateLimit(userId, clientIp, files);

        Feedback feedback = Feedback.builder()
                .user(user)
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

    /**
     * 특정 사용자가 작성한 문의 및 건의 목록을 페이징하여 조회합니다.
     */
    @Transactional(readOnly = true)
    public Page<FeedbackResponse> getMyFeedbacks(Long userId, Pageable pageable) {
        return feedbackRepository.findAllByUserId(userId, pageable)
                .map(FeedbackResponse::from);
    }

    /**
     * 특정 문의 사항의 상세 내용 및 운영진 답변 목록을 조회합니다.
     */
    @Transactional(readOnly = true)
    public FeedbackDetailResponse getMyFeedbackDetail(Long userId, Long feedbackId) {
        Feedback feedback = feedbackRepository.findById(feedbackId)
                .orElseThrow(() -> new CustomException(ErrorCode.FEEDBACK_NOT_FOUND));

        if (!feedback.getUser().getId().equals(userId)) {
            throw new CustomException(ErrorCode.FEEDBACK_UNAUTHORIZED);
        }

        return FeedbackDetailResponse.from(feedback);
    }

    /**
     * 문의 및 건의 게시글을 삭제 처리(소프트 삭제) 합니다.
     */
    @Transactional
    public void deleteFeedback(Long userId, Long feedbackId) {
        Feedback feedback = feedbackRepository.findById(feedbackId)
                .orElseThrow(() -> new CustomException(ErrorCode.FEEDBACK_NOT_FOUND));

        if (!feedback.getUser().getId().equals(userId)) {
            throw new CustomException(ErrorCode.FEEDBACK_UNAUTHORIZED);
        }

        feedback.delete();
    }

    @Transactional
    public FeedbackAttachmentDownload getAttachment(User requester, Long feedbackId, Long attachmentId) {
        if (requester.getRole().equals(Role.ADMIN)) {
            throw new CustomException(ErrorCode.FEEDBACK_UNAUTHORIZED);
        }

        Feedback feedback = feedbackRepository.findById(feedbackId)
                .orElseThrow(() -> new CustomException(ErrorCode.FEEDBACK_NOT_FOUND));
        FeedbackAttachment attachment = feedbackAttachmentRepository.findByIdAndFeedbackId(attachmentId, feedbackId)
                .orElseThrow(() -> new CustomException(ErrorCode.FEEDBACK_NOT_FOUND));
        if (!requester.getId().equals(feedback.getUser().getId())) {
            throw new CustomException(ErrorCode.FEEDBACK_UNAUTHORIZED);
        }
        var file = fileUploadService.loadFile(attachment.getFileUrl());
        return new FeedbackAttachmentDownload(file.resource(), attachment.getOriginalName(), file.contentType());
    }

    /**
     * 관리자는 명시적으로 확인한 뒤 전용 경로에서만 보존된 첨부파일을 열람합니다.
     */
    @Transactional
    public FeedbackAttachmentDownload getAttachmentForAdmin(User admin, Long feedbackId, Long attachmentId,
                                                            boolean confirmed) {
        if (!admin.getRole().equals(Role.ADMIN)) {
            throw new CustomException(ErrorCode.FEEDBACK_UNAUTHORIZED);
        }
        if (!confirmed) {
            throw new CustomException(ErrorCode.INVALID_INPUT, "첨부파일 열람을 확인해주세요.");
        }

        AdminFeedbackReadRepository.FeedbackAttachmentAccess attachment = adminFeedbackReadRepository
                .findAttachmentForAdmin(feedbackId, attachmentId)
                .orElseThrow(() -> new CustomException(ErrorCode.FEEDBACK_NOT_FOUND));
        var file = fileUploadService.loadFile(attachment.fileUrl());
        adminAuditService.recordAttachmentAccess(admin, attachmentId, feedbackId);
        return new FeedbackAttachmentDownload(file.resource(), attachment.originalName(), file.contentType());
    }

    /* 관리자 전용 기능 */

    /**
     * 관리자를 위해 시스템의 모든 문의 및 건의 목록을 조회합니다.
     */
    @Transactional(readOnly = true)
    public Page<AdminFeedbackResponse> getFeedbacksForAdmin(Pageable pageable,
                                                             AdminFeedbackDeletionFilter deletionFilter) {
        return adminFeedbackReadRepository.findFeedbacks(deletionFilter, pageable)
                .map(feedback -> new AdminFeedbackResponse(
                        feedback.id(),
                        feedback.type(),
                        feedback.title(),
                        feedback.status(),
                        feedback.createdAt(),
                        feedback.hasReplies(),
                        feedback.deleted(),
                        feedback.deletedAt(),
                        authorLabel(feedback.authorWithdrawn())));
    }

    /**
     * 관리자용 문의 및 건의 상세 조회 로직을 수행합니다.
     */
    @Transactional(readOnly = true)
    public AdminFeedbackDetailResponse getFeedbackDetailForAdmin(Long feedbackId) {
        AdminFeedbackReadRepository.FeedbackDetail feedback = adminFeedbackReadRepository
                .findFeedbackDetail(feedbackId)
                .orElseThrow(() -> new CustomException(ErrorCode.FEEDBACK_NOT_FOUND));
        return new AdminFeedbackDetailResponse(
                feedback.id(),
                feedback.type(),
                feedback.title(),
                feedback.content(),
                feedback.status(),
                feedback.createdAt(),
                feedback.deleted(),
                feedback.deletedAt(),
                authorLabel(feedback.authorWithdrawn()),
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

    /**
     * 문의 사항에 대한 운영진 답변을 생성하고 로그를 남깁니다.
     */
    @Transactional
    public Long createFeedbackReply(Long adminId, Long feedbackId, FeedbackReplyCreateRequest request) {
        User admin = userRepository.findById(adminId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        Feedback feedback = feedbackRepository.findById(feedbackId)
                .orElseThrow(() -> new CustomException(ErrorCode.FEEDBACK_NOT_FOUND));

        FeedbackReply reply = FeedbackReply.builder()
                .feedback(feedback)
                .admin(admin)
                .content(request.content())
                .build();

        FeedbackReply savedReply = feedbackReplyRepository.save(reply);

        adminAuditService.recordReplyCreated(admin, savedReply.getId(), feedbackId, request.content());

        return savedReply.getId();
    }

    /**
     * 기 등록된 운영진 답변 내용을 수정하고 변경 전 내용을 로그로 기록합니다.
     */
    @Transactional
    public void updateFeedbackReply(Long adminId, Long replyId, FeedbackReplyUpdateRequest request) {
        User admin = userRepository.findById(adminId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        FeedbackReply reply = feedbackReplyRepository.findById(replyId)
                .orElseThrow(() -> new CustomException(ErrorCode.FEEDBACK_REPLY_NOT_FOUND));
        Feedback feedback = feedbackRepository.findById(reply.getFeedback().getId())
                .orElseThrow(() -> new CustomException(ErrorCode.FEEDBACK_NOT_FOUND));

        String oldContent = reply.getContent();
        reply.updateContent(request.content());

        adminAuditService.recordReplyUpdated(admin, reply.getId(), feedback.getId(), oldContent, request.content());
    }

    /**
     * 등록된 운영진 답변을 보존용 soft delete 처리하고 액션 로그를 남깁니다.
     */
    @Transactional
    public void deleteFeedbackReply(Long adminId, Long replyId) {
        User admin = userRepository.findById(adminId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        FeedbackReply reply = feedbackReplyRepository.findById(replyId)
                .orElseThrow(() -> new CustomException(ErrorCode.FEEDBACK_REPLY_NOT_FOUND));
        Feedback feedback = feedbackRepository.findById(reply.getFeedback().getId())
                .orElseThrow(() -> new CustomException(ErrorCode.FEEDBACK_NOT_FOUND));

        String content = reply.getContent();
        reply.delete();

        adminAuditService.recordReplyDeleted(admin, replyId, feedback.getId(), content);
    }

    /**
     * 문의 사항의 처리 상태를 변경하고 액션 로그를 생성합니다.
     */
    @Transactional
    public void updateFeedbackStatus(Long adminId, Long feedbackId, FeedbackStatusUpdateRequest request) {
        User admin = userRepository.findById(adminId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        Feedback feedback = feedbackRepository.findById(feedbackId)
                .orElseThrow(() -> new CustomException(ErrorCode.FEEDBACK_NOT_FOUND));

        FeedbackStatus oldStatus = feedback.getStatus();
        feedback.updateStatus(request.status());

        adminAuditService.recordStatusChange(admin, feedbackId, oldStatus, request.status());
    }

    /**
     * 문의 및 건의 요청 빈도 제한을 검증합니다.
     */
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
}
