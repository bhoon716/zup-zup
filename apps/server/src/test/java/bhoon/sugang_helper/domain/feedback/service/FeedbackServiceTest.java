package bhoon.sugang_helper.domain.feedback.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import bhoon.sugang_helper.common.error.CustomException;
import bhoon.sugang_helper.common.error.ErrorCode;
import bhoon.sugang_helper.common.util.LocalFileUploadService;
import bhoon.sugang_helper.domain.feedback.dto.request.FeedbackCreateRequest;
import bhoon.sugang_helper.domain.feedback.dto.request.FeedbackStatusUpdateRequest;
import bhoon.sugang_helper.domain.feedback.entity.Feedback;
import bhoon.sugang_helper.domain.feedback.entity.enums.FeedbackStatus;
import bhoon.sugang_helper.domain.feedback.entity.enums.FeedbackType;
import bhoon.sugang_helper.domain.feedback.repository.AdminActionLogRepository;
import bhoon.sugang_helper.domain.feedback.repository.FeedbackAttachmentRepository;
import bhoon.sugang_helper.domain.feedback.repository.FeedbackReplyRepository;
import bhoon.sugang_helper.domain.feedback.repository.FeedbackRepository;
import bhoon.sugang_helper.domain.user.entity.User;
import bhoon.sugang_helper.domain.user.repository.UserRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;

@ExtendWith(MockitoExtension.class)
class FeedbackServiceTest {

    @Mock
    private FeedbackRepository feedbackRepository;
    @Mock
    private FeedbackAttachmentRepository feedbackAttachmentRepository;
    @Mock
    private FeedbackReplyRepository feedbackReplyRepository;
    @Mock
    private AdminActionLogRepository adminActionLogRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private LocalFileUploadService fileUploadService;

    @InjectMocks
    private FeedbackService feedbackService;

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
        verify(adminActionLogRepository).save(any());
    }
}
