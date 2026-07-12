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
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;

@ExtendWith(MockitoExtension.class)
class RedisServiceTest {

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
}
