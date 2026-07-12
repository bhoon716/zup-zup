package bhoon.sugang_helper.feedback.infra;

import static org.assertj.core.api.Assertions.assertThat;

import bhoon.sugang_helper.feedback.domain.ActionType;
import bhoon.sugang_helper.feedback.domain.AdminActionLog;
import bhoon.sugang_helper.feedback.domain.TargetType;
import bhoon.sugang_helper.user.domain.Role;
import bhoon.sugang_helper.user.domain.User;
import bhoon.sugang_helper.user.infra.UserJpaRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

@DataJpaTest
class AdminActionLogJpaRepositoryTest {

    @Autowired
    private AdminActionLogJpaRepository adminActionLogRepository;
    @Autowired
    private UserJpaRepository userRepository;

    @Test
    @DisplayName("감사 로그는 최신 createdAt·id 순으로 중복 없이 페이지를 나눈다.")
    void findAllByOrderByCreatedAtDescIdDesc_returnsStablePages() {
        User admin = userRepository.saveAndFlush(User.builder()
                .email("admin@example.com")
                .name("관리자")
                .role(Role.ADMIN)
                .build());
        adminActionLogRepository.saveAndFlush(actionLog(admin, 1L));
        adminActionLogRepository.saveAndFlush(actionLog(admin, 2L));
        adminActionLogRepository.saveAndFlush(actionLog(admin, 3L));

        Page<AdminActionLog> firstPage = adminActionLogRepository
                .findAllByOrderByCreatedAtDescIdDesc(PageRequest.of(0, 2));
        Page<AdminActionLog> secondPage = adminActionLogRepository
                .findAllByOrderByCreatedAtDescIdDesc(PageRequest.of(1, 2));

        assertThat(firstPage.getTotalElements()).isEqualTo(3);
        assertThat(firstPage.getContent()).extracting(AdminActionLog::getTargetId).containsExactly(3L, 2L);
        assertThat(secondPage.getContent()).extracting(AdminActionLog::getTargetId).containsExactly(1L);
    }

    private AdminActionLog actionLog(User admin, Long targetId) {
        return AdminActionLog.builder()
                .admin(admin)
                .actionType(ActionType.STATUS_CHANGE)
                .targetType(TargetType.FEEDBACK)
                .targetId(targetId)
                .metaData("{\"schema\":\"admin-action\",\"version\":1,\"event\":\"STATUS_CHANGED\",\"data\":{}}")
                .build();
    }
}
