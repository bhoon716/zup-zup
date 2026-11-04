package bhoon.sugang_helper.common.redis;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;

@ExtendWith(MockitoExtension.class)
class RedisServiceTest {

    private static final String VERIFICATION_KEY = "verification:test@example.com";

    @Mock
    private RedisTemplate<String, Object> redisTemplate;
    @InjectMocks
    private RedisService redisService;

    @Test
    void incrementSetsTheTtlInTheSameRedisScriptExecution() {
        when(redisTemplate.execute(any(), eq(List.of("feedback-rate")), eq("60000"))).thenReturn(2L);

        long count = redisService.increment("feedback-rate", Duration.ofMinutes(1));

        assertThat(count).isEqualTo(2L);
        verify(redisTemplate).execute(any(), eq(List.of("feedback-rate")), eq("60000"));
        verify(redisTemplate, never()).expire(any(), any());
    }

    @Test
    void compareAndDeleteVerificationValueUsesOneRedisScriptExecution() {
        when(redisTemplate.execute(any(), eq(List.of(VERIFICATION_KEY)), eq("verification-token")))
                .thenReturn(1L);

        boolean deleted = redisService.compareAndDeleteValues(VERIFICATION_KEY, "verification-token");

        assertThat(deleted).isTrue();
        ArgumentCaptor<RedisScript<Long>> scriptCaptor = ArgumentCaptor.captor();
        verify(redisTemplate).execute(scriptCaptor.capture(), eq(List.of(VERIFICATION_KEY)), eq("verification-token"));
        assertThat(scriptCaptor.getValue().getScriptAsString()).contains("GET", "DEL");
    }
}
