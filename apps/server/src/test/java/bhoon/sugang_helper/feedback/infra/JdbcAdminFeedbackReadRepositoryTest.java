package bhoon.sugang_helper.feedback.infra;

import static org.assertj.core.api.Assertions.assertThat;

import bhoon.sugang_helper.feedback.domain.AdminFeedbackDeletionFilter;
import bhoon.sugang_helper.feedback.domain.AdminFeedbackReadRepository;
import bhoon.sugang_helper.feedback.domain.Feedback;
import bhoon.sugang_helper.feedback.domain.FeedbackAttachment;
import bhoon.sugang_helper.feedback.domain.FeedbackReply;
import bhoon.sugang_helper.feedback.domain.FeedbackType;
import bhoon.sugang_helper.user.domain.Role;
import bhoon.sugang_helper.user.domain.User;
import bhoon.sugang_helper.user.infra.UserJpaRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;

@DataJpaTest
@Import(JdbcAdminFeedbackReadRepository.class)
class JdbcAdminFeedbackReadRepositoryTest {

    @Autowired
    private AdminFeedbackReadRepository adminFeedbackReadRepository;
    @Autowired
    private FeedbackJpaRepository feedbackRepository;
    @Autowired
    private FeedbackReplyJpaRepository feedbackReplyRepository;
    @Autowired
    private FeedbackAttachmentJpaRepository feedbackAttachmentRepository;
    @Autowired
    private UserJpaRepository userRepository;
    @Autowired
    private EntityManager entityManager;

    @Test
    @DisplayName("관리자 읽기 모델은 삭제 피드백·답변을 포함하고 일반 JPA 조회는 계속 숨긴다.")
    void readsDeletedFeedbackAndRepliesOnlyThroughTheAdminProjection() {
        User activeUser = userRepository.saveAndFlush(user("active@example.com", Role.USER));
        User withdrawnUser = userRepository.saveAndFlush(user("withdrawn@example.com", Role.USER));
        User admin = userRepository.saveAndFlush(user("admin@example.com", Role.ADMIN));
        withdrawnUser.withdraw();
        userRepository.saveAndFlush(withdrawnUser);

        Feedback activeFeedback = feedbackRepository.saveAndFlush(feedback(activeUser, "활성 문의"));
        Feedback deletedFeedback = feedbackRepository.saveAndFlush(feedback(withdrawnUser, "삭제 문의"));
        FeedbackReply deletedReply = feedbackReplyRepository.saveAndFlush(FeedbackReply.builder()
                .feedback(deletedFeedback)
                .admin(admin)
                .content("삭제된 답변")
                .build());
        FeedbackAttachment attachment = feedbackAttachmentRepository.saveAndFlush(FeedbackAttachment.builder()
                .feedback(deletedFeedback)
                .fileUrl("/uploads/deleted.png")
                .originalName("private-name.png")
                .build());
        deletedFeedback.delete();
        deletedReply.delete();
        feedbackRepository.saveAndFlush(deletedFeedback);
        feedbackReplyRepository.saveAndFlush(deletedReply);
        entityManager.clear();

        var all = adminFeedbackReadRepository.findFeedbacks(AdminFeedbackDeletionFilter.ALL, PageRequest.of(0, 10));
        var activeOnly = adminFeedbackReadRepository.findFeedbacks(
                AdminFeedbackDeletionFilter.ACTIVE, PageRequest.of(0, 10));
        var deletedOnly = adminFeedbackReadRepository.findFeedbacks(
                AdminFeedbackDeletionFilter.DELETED, PageRequest.of(0, 10));
        var detail = adminFeedbackReadRepository.findFeedbackDetail(deletedFeedback.getId()).orElseThrow();

        assertThat(all.getContent()).extracting(AdminFeedbackReadRepository.FeedbackSummary::id)
                .containsExactlyInAnyOrder(activeFeedback.getId(), deletedFeedback.getId());
        assertThat(activeOnly.getContent()).extracting(AdminFeedbackReadRepository.FeedbackSummary::id)
                .containsExactly(activeFeedback.getId());
        assertThat(deletedOnly.getContent()).singleElement().satisfies(item -> {
            assertThat(item.id()).isEqualTo(deletedFeedback.getId());
            assertThat(item.deleted()).isTrue();
            assertThat(item.authorWithdrawn()).isTrue();
        });
        assertThat(detail.deleted()).isTrue();
        assertThat(adminFeedbackReadRepository.findReplies(deletedFeedback.getId()))
                .singleElement()
                .satisfies(reply -> {
                    assertThat(reply.id()).isEqualTo(deletedReply.getId());
                    assertThat(reply.deleted()).isTrue();
                    assertThat(reply.content()).isEqualTo("삭제된 답변");
                });
        assertThat(adminFeedbackReadRepository.findAttachments(deletedFeedback.getId()))
                .extracting(AdminFeedbackReadRepository.FeedbackAttachmentSummary::id)
                .hasSize(1);
        assertThat(adminFeedbackReadRepository.findAttachmentForAdmin(deletedFeedback.getId(), attachment.getId()))
                .hasValueSatisfying(access -> {
                    assertThat(access.fileUrl()).isEqualTo("/uploads/deleted.png");
                    assertThat(access.originalName()).isEqualTo("private-name.png");
                });
        assertThat(adminFeedbackReadRepository.findAttachmentForAdmin(activeFeedback.getId(), attachment.getId())).isEmpty();
        assertThat(feedbackAttachmentRepository.findById(attachment.getId())).isPresent();
        assertThat(feedbackRepository.findById(deletedFeedback.getId())).isEmpty();
        assertThat(feedbackRepository.findAll()).extracting(Feedback::getId).containsExactly(activeFeedback.getId());
        assertThat(feedbackReplyRepository.findById(deletedReply.getId())).isEmpty();
    }

    private User user(String email, Role role) {
        return User.builder().email(email).name("사용자").role(role).build();
    }

    private Feedback feedback(User user, String title) {
        return Feedback.builder()
                .user(user)
                .type(FeedbackType.BUG)
                .title(title)
                .content("내용")
                .metaInfo("{\"url\":\"https://private.example\"}")
                .build();
    }
}
