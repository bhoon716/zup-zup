package bhoon.sugang_helper.support.feedback.repository;

import static org.assertj.core.api.Assertions.assertThat;

import bhoon.sugang_helper.identity.user.entity.User;
import bhoon.sugang_helper.identity.user.enums.Role;
import bhoon.sugang_helper.identity.user.repository.UserRepository;
import bhoon.sugang_helper.support.feedback.dto.request.AdminFeedbackSearch;
import bhoon.sugang_helper.support.feedback.dto.result.AdminFeedbackAttachmentSummary;
import bhoon.sugang_helper.support.feedback.dto.result.AdminFeedbackSummary;
import bhoon.sugang_helper.support.feedback.entity.Feedback;
import bhoon.sugang_helper.support.feedback.entity.FeedbackAttachment;
import bhoon.sugang_helper.support.feedback.entity.FeedbackReply;
import bhoon.sugang_helper.support.feedback.enums.AdminFeedbackDeletionFilter;
import bhoon.sugang_helper.support.feedback.enums.AdminFeedbackOrder;
import bhoon.sugang_helper.support.feedback.enums.AdminFeedbackSearchType;
import bhoon.sugang_helper.support.feedback.enums.FeedbackStatus;
import bhoon.sugang_helper.support.feedback.enums.FeedbackType;
import jakarta.persistence.EntityManager;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;

@DataJpaTest
@Import(AdminFeedbackReadRepository.class)
class AdminFeedbackReadRepositoryTest {

    private static final AdminFeedbackSearch NO_SEARCH = new AdminFeedbackSearch(null, AdminFeedbackSearchType.TITLE_CONTENT);

    @Autowired
    private AdminFeedbackReadRepository adminFeedbackReadRepository;

    @Autowired
    private FeedbackRepository feedbackRepository;

    @Autowired
    private FeedbackReplyRepository feedbackReplyRepository;

    @Autowired
    private FeedbackAttachmentRepository feedbackAttachmentRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    @DisplayName("관리자 읽기 모델은 삭제 피드백·답변을 포함하고 일반 JPA 조회는 계속 숨긴다.")
    void readsDeletedFeedbackAndRepliesOnlyThroughTheAdminProjection() {
        // given
        User activeUser = userRepository.saveAndFlush(user("active@example.com", Role.USER));
        User withdrawnUser = userRepository.saveAndFlush(user("withdrawn@example.com", Role.USER));
        User admin = userRepository.saveAndFlush(user("admin@example.com", Role.ADMIN));
        withdrawnUser.withdraw();
        userRepository.saveAndFlush(withdrawnUser);

        Feedback activeFeedback = feedbackRepository.saveAndFlush(feedback(activeUser, "활성 문의"));
        Feedback deletedFeedback = feedbackRepository.saveAndFlush(feedback(withdrawnUser, "삭제 문의"));
        FeedbackReply deletedReply = feedbackReplyRepository.saveAndFlush(FeedbackReply.builder()
                .feedback(deletedFeedback)
                .adminId(admin.getId())
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

        // when
        var all = adminFeedbackReadRepository.findFeedbacks(
                AdminFeedbackDeletionFilter.ALL, AdminFeedbackOrder.NEWEST, NO_SEARCH, PageRequest.of(0, 10));
        var activeOnly = adminFeedbackReadRepository.findFeedbacks(
                AdminFeedbackDeletionFilter.ACTIVE, AdminFeedbackOrder.NEWEST, NO_SEARCH, PageRequest.of(0, 10));
        var deletedOnly = adminFeedbackReadRepository.findFeedbacks(
                AdminFeedbackDeletionFilter.DELETED, AdminFeedbackOrder.NEWEST, NO_SEARCH, PageRequest.of(0, 10));
        var detail = adminFeedbackReadRepository.findFeedbackDetail(deletedFeedback.getId()).orElseThrow();

        // then
        assertThat(all.getContent()).extracting(AdminFeedbackSummary::id)
                .containsExactlyInAnyOrder(activeFeedback.getId(), deletedFeedback.getId());
        assertThat(activeOnly.getContent()).extracting(AdminFeedbackSummary::id)
                .containsExactly(activeFeedback.getId());
        assertThat(deletedOnly.getContent()).singleElement().satisfies(item -> {
            assertThat(item.id()).isEqualTo(deletedFeedback.getId());
            assertThat(item.deleted()).isTrue();
            assertThat(item.authorId()).isEqualTo(withdrawnUser.getId());
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
                .extracting(AdminFeedbackAttachmentSummary::id)
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

    @Test
    @DisplayName("관리자 문의 정렬은 페이지를 나누기 전에 적용하고 같은 시각에는 ID를 사용한다")
    void sortsBeforePagination() {
        // given
        User author = userRepository.saveAndFlush(user("author@example.com", Role.USER));
        Feedback olderPending = feedbackRepository.saveAndFlush(feedback(author, "첫 번째 문의"));
        Feedback newerPending = feedbackRepository.saveAndFlush(feedback(author, "두 번째 문의"));
        Feedback completed = feedback(author, "처리한 문의");
        completed.updateStatus(FeedbackStatus.COMPLETED);
        feedbackRepository.saveAndFlush(completed);

        LocalDateTime firstDay = LocalDateTime.of(2026, 1, 1, 12, 0);
        entityManager.createNativeQuery("UPDATE feedbacks SET created_at = :createdAt")
                .setParameter("createdAt", firstDay)
                .executeUpdate();
        entityManager.createNativeQuery("UPDATE feedbacks SET created_at = :createdAt WHERE id = :id")
                .setParameter("createdAt", firstDay.plusDays(1))
                .setParameter("id", completed.getId())
                .executeUpdate();

        // when & then
        assertThat(adminFeedbackReadRepository.findFeedbacks(
                AdminFeedbackDeletionFilter.ACTIVE, AdminFeedbackOrder.PENDING_FIRST, NO_SEARCH, PageRequest.of(0, 1)))
                .extracting(AdminFeedbackSummary::id)
                .containsExactly(newerPending.getId());
        assertThat(adminFeedbackReadRepository.findFeedbacks(
                AdminFeedbackDeletionFilter.ACTIVE, AdminFeedbackOrder.PENDING_FIRST, NO_SEARCH, PageRequest.of(1, 1)))
                .extracting(AdminFeedbackSummary::id)
                .containsExactly(olderPending.getId());
        assertThat(adminFeedbackReadRepository.findFeedbacks(
                AdminFeedbackDeletionFilter.ACTIVE, AdminFeedbackOrder.NEWEST, NO_SEARCH, PageRequest.of(0, 3)))
                .extracting(AdminFeedbackSummary::id)
                .containsExactly(completed.getId(), newerPending.getId(), olderPending.getId());
        assertThat(adminFeedbackReadRepository.findFeedbacks(
                AdminFeedbackDeletionFilter.ACTIVE, AdminFeedbackOrder.OLDEST, NO_SEARCH, PageRequest.of(0, 3)))
                .extracting(AdminFeedbackSummary::id)
                .containsExactly(olderPending.getId(), newerPending.getId(), completed.getId());
    }

    @Test
    @DisplayName("검색 범위와 삭제 필터에 맞는 문의만 페이지와 전체 건수에 포함한다")
    void searchesAcrossTheListWithoutDuplicatingReplyMatches() {
        // given
        User author = userRepository.saveAndFlush(user("author@example.com", Role.USER));
        User admin = userRepository.saveAndFlush(user("admin@example.com", Role.ADMIN));
        var titleMatch = feedbackRepository.saveAndFlush(feedback(author, "첨부 REF%_ 확인"));
        var contentMatch = feedbackRepository.saveAndFlush(feedback(author, "본문 일치", "ref%_ 재현"));
        var replyMatch = feedbackRepository.saveAndFlush(feedback(author, "답변 일치"));
        var deletedFeedback = feedbackRepository.saveAndFlush(feedback(author, "삭제 문의"));
        reply(admin, replyMatch, "ref%_ 안내");
        reply(admin, replyMatch, "ref%_ 확인");
        var deletedReply = reply(admin, deletedFeedback, "ref%_ 삭제 답변");
        deletedFeedback.delete();
        deletedReply.delete();
        feedbackRepository.saveAndFlush(deletedFeedback);
        feedbackReplyRepository.saveAndFlush(deletedReply);
        feedbackRepository.saveAndFlush(feedback(author, "첨부 ref12 확인"));

        var keyword = "  ref%_  ";
        var allFields = new AdminFeedbackSearch(keyword, AdminFeedbackSearchType.TITLE_CONTENT_REPLY);
        var oldest = AdminFeedbackOrder.OLDEST;
        var active = AdminFeedbackDeletionFilter.ACTIVE;
        var firstPage = PageRequest.of(0, 10);

        // when & then
        assertThat(adminFeedbackReadRepository.findFeedbacks(
                active, oldest, new AdminFeedbackSearch(keyword, AdminFeedbackSearchType.TITLE), firstPage))
                .extracting(AdminFeedbackSummary::id)
                .containsExactly(titleMatch.getId());
        assertThat(adminFeedbackReadRepository.findFeedbacks(
                active, oldest, new AdminFeedbackSearch(keyword, AdminFeedbackSearchType.TITLE_CONTENT), firstPage))
                .extracting(AdminFeedbackSummary::id)
                .containsExactly(titleMatch.getId(), contentMatch.getId());
        var activePage = adminFeedbackReadRepository.findFeedbacks(active, oldest, allFields, PageRequest.of(1, 2));
        assertThat(activePage).extracting(AdminFeedbackSummary::id).containsExactly(replyMatch.getId());
        assertThat(activePage.getTotalElements()).isEqualTo(3);
        assertThat(activePage.getTotalPages()).isEqualTo(2);

        var deletedPage = adminFeedbackReadRepository.findFeedbacks(
                AdminFeedbackDeletionFilter.DELETED, oldest, allFields, firstPage);
        assertThat(deletedPage).extracting(AdminFeedbackSummary::id).containsExactly(deletedFeedback.getId());
        assertThat(deletedPage.getTotalElements()).isEqualTo(1);
    }

    private User user(String email, Role role) {
        return User.builder().email(email).name("사용자").role(role).build();
    }

    private Feedback feedback(User user, String title) {
        return feedback(user, title, "내용");
    }

    private Feedback feedback(User user, String title, String content) {
        return Feedback.builder()
                .userId(user.getId())
                .type(FeedbackType.BUG)
                .title(title)
                .content(content)
                .metaInfo("{\"url\":\"https://private.example\"}")
                .build();
    }

    private FeedbackReply reply(User admin, Feedback feedback, String content) {
        return feedbackReplyRepository.saveAndFlush(FeedbackReply.builder()
                .feedback(feedback)
                .adminId(admin.getId())
                .content(content)
                .build());
    }
}
