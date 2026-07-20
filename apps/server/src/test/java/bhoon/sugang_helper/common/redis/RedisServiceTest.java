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

    private static final String REFRESH_TOKEN_KEY = "RT:test@example.com";

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
    void compareAndSetRefreshRecordUsesOneRedisScriptExecution() {
        when(redisTemplate.execute(any(), eq(List.of(REFRESH_TOKEN_KEY)),
                eq("v2:family:old-digest"), eq("v2:family:new-digest"), eq("1209600000")))
                .thenReturn(1L);

        boolean updated = redisService.compareAndSetValues(REFRESH_TOKEN_KEY, "v2:family:old-digest",
                "v2:family:new-digest", Duration.ofDays(14));

        assertThat(updated).isTrue();
        ArgumentCaptor<RedisScript<Long>> scriptCaptor = ArgumentCaptor.captor();
        verify(redisTemplate).execute(scriptCaptor.capture(), eq(List.of(REFRESH_TOKEN_KEY)),
                eq("v2:family:old-digest"), eq("v2:family:new-digest"), eq("1209600000"));
        assertThat(scriptCaptor.getValue().getScriptAsString()).contains("GET", "SET", "PX");
    }

    @Test
    void compareAndDeleteRefreshRecordUsesOneRedisScriptExecution() {
        when(redisTemplate.execute(any(), eq(List.of(REFRESH_TOKEN_KEY)), eq("v2:family:digest")))
                .thenReturn(1L);

        boolean deleted = redisService.compareAndDeleteValues(REFRESH_TOKEN_KEY, "v2:family:digest");

        assertThat(deleted).isTrue();
        ArgumentCaptor<RedisScript<Long>> scriptCaptor = ArgumentCaptor.captor();
        verify(redisTemplate).execute(scriptCaptor.capture(), eq(List.of(REFRESH_TOKEN_KEY)), eq("v2:family:digest"));
        assertThat(scriptCaptor.getValue().getScriptAsString()).contains("GET", "DEL");
    }
}
