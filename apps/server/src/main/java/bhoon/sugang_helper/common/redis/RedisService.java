package bhoon.sugang_helper.common.redis;

import java.time.Duration;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RedisService {

    private static final DefaultRedisScript<Long> ATOMIC_INCREMENT_WITH_TTL = new DefaultRedisScript<>(
            "local count = redis.call('INCR', KEYS[1]); "
                    + "if count == 1 then redis.call('PEXPIRE', KEYS[1], ARGV[1]); end; return count;",
            Long.class);
    private static final DefaultRedisScript<Long> COMPARE_AND_SET_WITH_TTL = new DefaultRedisScript<>(
            "if redis.call('GET', KEYS[1]) == ARGV[1] then "
                    + "redis.call('SET', KEYS[1], ARGV[2], 'PX', ARGV[3]); return 1; end; return 0;",
            Long.class);
    private static final DefaultRedisScript<Long> COMPARE_AND_DELETE = new DefaultRedisScript<>(
            "if redis.call('GET', KEYS[1]) == ARGV[1] then redis.call('DEL', KEYS[1]); return 1; end; return 0;",
            Long.class);
    private final RedisTemplate<String, Object> redisTemplate;

    /**
     * 특정 키에 대한 값을 Redis에 저장합니다.
     */
    public void setValues(String key, String data) {
        ValueOperations<String, Object> values = redisTemplate.opsForValue();
        values.set(key, data);
    }

    /**
     * 만료 시간을 설정하여 특정 키에 대한 값을 Redis에 저장합니다.
     */
    public void setValues(String key, String data, Duration duration) {
        ValueOperations<String, Object> values = redisTemplate.opsForValue();
        values.set(key, data, duration);
    }

    /**
     * 해당 키가 없을 경우에만 값을 저장하며, 만료 시간을 설정합니다.
     */
    public boolean setValuesIfAbsent(String key, String data, Duration duration) {
        ValueOperations<String, Object> values = redisTemplate.opsForValue();
        return Boolean.TRUE.equals(values.setIfAbsent(key, data, duration));
    }

    /**
     * Redis에서 특정 키에 해당하는 값을 조회합니다.
     */
    public String getValues(String key) {
        ValueOperations<String, Object> values = redisTemplate.opsForValue();
        Object object = values.get(key);
        return object == null ? null : object.toString();
    }

    /**
     * Redis에서 특정 키와 데이터를 삭제합니다.
     */
    public void deleteValues(String key) {
        redisTemplate.delete(key);
    }

    /**
     * Redis에 해당 키가 존재하는지 확인합니다.
     */
    public boolean hasKey(String key) {
        return redisTemplate.hasKey(key);
    }

    public long increment(String key, Duration duration) {
        Long value = redisTemplate.execute(ATOMIC_INCREMENT_WITH_TTL, java.util.List.of(key),
                String.valueOf(duration.toMillis()));
        return value == null ? 0 : value;
    }

    /**
     * 현재 값이 expectedValue일 때만 새 값과 TTL을 원자적으로 저장합니다.
     */
    public boolean compareAndSetValues(String key, String expectedValue, String newValue, Duration duration) {
        Long value = redisTemplate.execute(COMPARE_AND_SET_WITH_TTL, java.util.List.of(key), expectedValue, newValue,
                String.valueOf(duration.toMillis()));
        return Long.valueOf(1L).equals(value);
    }

    /**
     * 현재 값이 expectedValue일 때만 원자적으로 삭제합니다.
     */
    public boolean compareAndDeleteValues(String key, String expectedValue) {
        Long value = redisTemplate.execute(COMPARE_AND_DELETE, java.util.List.of(key), expectedValue);
        return Long.valueOf(1L).equals(value);
    }
}
