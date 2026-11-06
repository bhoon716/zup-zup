package bhoon.sugang_helper.common.redis;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
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
    @DisplayName("Redis 값 증가와 만료 시간 설정은 한 스크립트에서 실행한다")
    void incrementSetsTheTtlInTheSameRedisScriptExecution() {
        // given
        when(redisTemplate.execute(any(), eq(List.of("feedback-rate")), eq("60000"))).thenReturn(2L);

        // when
        long count = redisService.increment("feedback-rate", Duration.ofMinutes(1));

        // then
        assertThat(count).isEqualTo(2L);
        verify(redisTemplate).execute(any(), eq(List.of("feedback-rate")), eq("60000"));
        verify(redisTemplate, never()).expire(any(), any());
    }

    @Test
    @DisplayName("인증 값 비교와 삭제는 한 Redis 스크립트에서 실행한다")
    void compareAndDeleteVerificationValueUsesOneRedisScriptExecution() {
        // given
        when(redisTemplate.execute(any(), eq(List.of(VERIFICATION_KEY)), eq("verification-token")))
                .thenReturn(1L);

        // when
        boolean deleted = redisService.compareAndDeleteValues(VERIFICATION_KEY, "verification-token");

        // then
        assertThat(deleted).isTrue();
        ArgumentCaptor<RedisScript<Long>> scriptCaptor = ArgumentCaptor.captor();
        verify(redisTemplate).execute(scriptCaptor.capture(), eq(List.of(VERIFICATION_KEY)), eq("verification-token"));
        assertThat(scriptCaptor.getValue().getScriptAsString()).contains("GET", "DEL");
    }
}
