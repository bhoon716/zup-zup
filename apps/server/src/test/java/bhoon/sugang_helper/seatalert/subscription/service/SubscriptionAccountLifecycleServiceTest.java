package bhoon.sugang_helper.seatalert.subscription.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import bhoon.sugang_helper.seatalert.subscription.entity.Subscription;
import bhoon.sugang_helper.seatalert.subscription.repository.SubscriptionRepository;
import java.util.List;
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
    void cancelAllForUserDeactivatesExistingSubscriptions() {
        Subscription subscription = Subscription.builder()
                .userId(7L)
                .courseKey("CS101")
                .isActive(true)
                .build();
        when(subscriptionRepository.findByUserId(7L)).thenReturn(List.of(subscription));

        lifecycleService.cancelAllForUser(7L);

        assertThat(subscription.isActive()).isFalse();
    }
}
