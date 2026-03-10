package bhoon.sugang_helper.domain.feedback.service;

import bhoon.sugang_helper.common.error.CustomException;
import bhoon.sugang_helper.common.error.ErrorCode;
import bhoon.sugang_helper.common.util.LocalFileUploadService;
import bhoon.sugang_helper.domain.feedback.dto.request.FeedbackCreateRequest;
import bhoon.sugang_helper.domain.feedback.dto.request.FeedbackReplyCreateRequest;
import bhoon.sugang_helper.domain.feedback.dto.request.FeedbackReplyUpdateRequest;
import bhoon.sugang_helper.domain.feedback.dto.request.FeedbackStatusUpdateRequest;
import bhoon.sugang_helper.domain.feedback.dto.response.FeedbackDetailResponse;
import bhoon.sugang_helper.domain.feedback.dto.response.FeedbackResponse;
import bhoon.sugang_helper.domain.feedback.entity.AdminActionLog;
import bhoon.sugang_helper.domain.feedback.entity.Feedback;
import bhoon.sugang_helper.domain.feedback.entity.FeedbackAttachment;
import bhoon.sugang_helper.domain.feedback.entity.FeedbackReply;
import bhoon.sugang_helper.domain.feedback.entity.enums.ActionType;
import bhoon.sugang_helper.domain.feedback.entity.enums.FeedbackStatus;
import bhoon.sugang_helper.domain.feedback.entity.enums.TargetType;
import bhoon.sugang_helper.domain.feedback.repository.AdminActionLogRepository;
import bhoon.sugang_helper.domain.feedback.repository.FeedbackAttachmentRepository;
import bhoon.sugang_helper.domain.feedback.repository.FeedbackReplyRepository;
import bhoon.sugang_helper.domain.feedback.repository.FeedbackRepository;
import bhoon.sugang_helper.domain.user.entity.User;
import bhoon.sugang_helper.domain.user.repository.UserRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/**
 * 문의 및 건의사항(버그 리포트, 기능 제안 등) 관련 비즈니스 로직을 처리하는 서비스 클래스입니다.
 * 사용자의 의견 등록, 조회 및 관리자의 답변 관리 기능을 담당합니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FeedbackService {

        private final FeedbackRepository feedbackRepository;
        private final FeedbackAttachmentRepository feedbackAttachmentRepository;
        private final FeedbackReplyRepository feedbackReplyRepository;
        private final AdminActionLogRepository adminActionLogRepository;
        private final UserRepository userRepository;
        private final LocalFileUploadService fileUploadService;

        /**
         * 사용자의 새로운 문의 및 건의사항을 등록하고 첨부 파일을 저장합니다.
         */
        @Transactional
        public Long createFeedback(Long userId, FeedbackCreateRequest request, List<MultipartFile> files) {
                User user = userRepository.findById(userId)
                                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

                validateRateLimit(user);

                Feedback feedback = Feedback.builder()
                                .user(user)
                                .type(request.type())
                                .title(request.title())
                                .content(request.content())
                                .metaInfo(request.metaInfo())
                                .build();

                Feedback savedFeedback = feedbackRepository.save(feedback);

                if (files != null && !files.isEmpty()) {
                        List<String> fileUrls = fileUploadService.uploadImages(files);

                        for (int i = 0; i < fileUrls.size(); i++) {
                                String originalName = files.get(i).getOriginalFilename();
                                FeedbackAttachment attachment = FeedbackAttachment.builder()
                                                .feedback(savedFeedback)
                                                .fileUrl(fileUrls.get(i))
                                                .originalName(originalName != null ? originalName : "unknown")
                                                .build();
                                feedbackAttachmentRepository.save(attachment);
                        }
                }

                return savedFeedback.getId();
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

        /* 관리자 전용 기능 */

        /**
         * 관리자를 위해 시스템의 모든 문의 및 건의 목록을 조회합니다.
         */
        @Transactional(readOnly = true)
        public Page<FeedbackResponse> getFeedbacksForAdmin(Pageable pageable) {
                return feedbackRepository.findAll(pageable)
                                .map(FeedbackResponse::from);
        }

        /**
         * 관리자용 문의 및 건의 상세 조회 로직을 수행합니다.
         */
        @Transactional(readOnly = true)
        public FeedbackDetailResponse getFeedbackDetailForAdmin(Long feedbackId) {
                Feedback feedback = feedbackRepository.findById(feedbackId)
                                .orElseThrow(() -> new CustomException(ErrorCode.FEEDBACK_NOT_FOUND));
                return FeedbackDetailResponse.from(feedback);
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

                logAdminAction(admin, ActionType.REPLY_CREATE, TargetType.REPLY, savedReply.getId(), null);

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

                String oldContent = reply.getContent();
                reply.updateContent(request.content());

                logAdminAction(admin, ActionType.REPLY_UPDATE, TargetType.REPLY, reply.getId(),
                                "{\"oldContent\":\"" + oldContent + "\"}");
        }

        /**
         * 등록된 운영진 답변을 영구 삭제하고 액션 로그를 남깁니다.
         */
        @Transactional
        public void deleteFeedbackReply(Long adminId, Long replyId) {
                User admin = userRepository.findById(adminId)
                                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

                FeedbackReply reply = feedbackReplyRepository.findById(replyId)
                                .orElseThrow(() -> new CustomException(ErrorCode.FEEDBACK_REPLY_NOT_FOUND));

                feedbackReplyRepository.delete(reply);

                logAdminAction(admin, ActionType.REPLY_DELETE, TargetType.REPLY, replyId, null);
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

                logAdminAction(admin, ActionType.STATUS_CHANGE, TargetType.FEEDBACK, feedback.getId(),
                                "{\"oldStatus\":\"" + oldStatus + "\", \"newStatus\":\"" + request.status() + "\"}");
        }

        /**
         * 운영진의 액션(답변 등록/수정/삭제, 상태 변경 등)을 히스토리 형식으로 저장합니다.
         */
        private void logAdminAction(User admin, ActionType actionType, TargetType targetType, Long targetId,
                        String metaData) {
                AdminActionLog actionLog = AdminActionLog.builder()
                                .admin(admin)
                                .actionType(actionType)
                                .targetType(targetType)
                                .targetId(targetId)
                                .metaData(metaData)
                                .build();
                adminActionLogRepository.save(actionLog);
        }

        /**
         * 문의 및 건의 요청 빈도 제한을 검증합니다.
         */
        private void validateRateLimit(User user) {
                // 일회성 로직 구현 생략 (Redis 등 연동 권장)
        }
}
