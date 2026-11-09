package bhoon.sugang_helper.seatalert.subscription.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import bhoon.sugang_helper.identity.events.UserWithdrawnEvent;
import bhoon.sugang_helper.seatalert.subscription.entity.Subscription;
import bhoon.sugang_helper.seatalert.subscription.repository.SubscriptionRepository;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SubscriptionAccountLifecycleServiceTest {

    @Mock
    private SubscriptionRepository subscriptionRepository;

    @InjectMocks
    private SubscriptionAccountLifecycleService lifecycleService;

    @Test
    @DisplayName("탈퇴 사용자의 기존 구독을 비활성화한다")
    void cancelAllForUserDeactivatesExistingSubscriptions() {
        // given
        Subscription subscription = Subscription.builder()
                .userId(7L)
                .courseKey("CS101")
                .isActive(true)
                .build();
        when(subscriptionRepository.findByUserId(7L)).thenReturn(List.of(subscription));

        // when
        lifecycleService.onUserWithdrawn(new UserWithdrawnEvent(7L));

        // then
        assertThat(subscription.isActive()).isFalse();
    }
}
