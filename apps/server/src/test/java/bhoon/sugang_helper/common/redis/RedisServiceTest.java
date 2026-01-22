package bhoon.sugang_helper.common.redis;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import java.time.Duration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

@ExtendWith(MockitoExtension.class)
class RedisServiceTest {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    @InjectMocks
    private RedisService redisService;

    @Test
    @DisplayName("Redis 값 저장 테스트")
    void setValues() {
        // given
        String key = "key";
        String value = "value";
        given(redisTemplate.opsForValue()).willReturn(valueOperations);

        // when
        redisService.setValues(key, value);

        // then
        verify(valueOperations).set(key, value);
    }

    @Test
    @DisplayName("Redis 값 저장 (만료시간 포함) 테스트")
    void setValuesWithDuration() {
        // given
        String key = "key";
        String value = "value";
        Duration duration = Duration.ofMinutes(1);
        given(redisTemplate.opsForValue()).willReturn(valueOperations);

        // when
        redisService.setValues(key, value, duration);

        // then
        verify(valueOperations).set(key, value, duration);
    }

    @Test
    @DisplayName("Redis 값 조회 테스트")
    void getValues() {
        // given
        String key = "key";
        String value = "value";
        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        given(valueOperations.get(key)).willReturn(value);

        // when
        String result = redisService.getValues(key);

        // then
        assertThat(result).isEqualTo(value);
    }

    @Test
    @DisplayName("Redis 값 삭제 테스트")
    void deleteValues() {
        // given
        String key = "key";

        // when
        redisService.deleteValues(key);

        // then
        verify(redisTemplate).delete(key);
    }
}
