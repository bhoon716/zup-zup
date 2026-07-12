package bhoon.sugang_helper.user.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import bhoon.sugang_helper.user.infra.UserJpaRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

@DataJpaTest
class UserWithdrawalOptimisticLockingTest {

    @Autowired
    private UserJpaRepository userRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void staleProfileUpdateCannotRestoreAWithdrawnAccount() {
        TransactionTemplate transactions = new TransactionTemplate(transactionManager);
        transactions.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        User savedUser = transactions.execute(status -> userRepository.saveAndFlush(User.builder()
                .email("race@example.com")
                .name("기존 사용자")
                .role(Role.USER)
                .build()));
        User staleUser = transactions.execute(status -> userRepository.findById(savedUser.getId()).orElseThrow());

        transactions.executeWithoutResult(status -> {
            User withdrawingUser = userRepository.findById(savedUser.getId()).orElseThrow();
            withdrawingUser.withdraw();
            userRepository.flush();
        });

        staleUser.update("동시 프로필 변경");

        assertThatThrownBy(() -> transactions.executeWithoutResult(status -> userRepository.saveAndFlush(staleUser)))
                .isInstanceOf(ObjectOptimisticLockingFailureException.class);
        User persistedUser = transactions.execute(status -> userRepository.findById(savedUser.getId()).orElseThrow());
        assertThat(persistedUser.isDeleted()).isTrue();
        assertThat(persistedUser.getEmail()).isEqualTo("deleted-" + savedUser.getId() + "@deleted.invalid");
    }
}
