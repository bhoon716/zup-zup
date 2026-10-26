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
    private static final DefaultRedisScript<Long> COMPARE_AND_DELETE = new DefaultRedisScript<>(
            "if redis.call('GET', KEYS[1]) == ARGV[1] then redis.call('DEL', KEYS[1]); return 1; end; return 0;",
            Long.class);
    private final RedisTemplate<String, Object> redisTemplate;




    public void setValues(String key, String data) {
        ValueOperations<String, Object> values = redisTemplate.opsForValue();
        values.set(key, data);
    }




    public void setValues(String key, String data, Duration duration) {
        ValueOperations<String, Object> values = redisTemplate.opsForValue();
        values.set(key, data, duration);
    }




    public boolean setValuesIfAbsent(String key, String data, Duration duration) {
        ValueOperations<String, Object> values = redisTemplate.opsForValue();
        return Boolean.TRUE.equals(values.setIfAbsent(key, data, duration));
    }




    public String getValues(String key) {
        ValueOperations<String, Object> values = redisTemplate.opsForValue();
        Object object = values.get(key);
        return object == null ? null : object.toString();
    }




    public void deleteValues(String key) {
        redisTemplate.delete(key);
    }




    public boolean hasKey(String key) {
        return redisTemplate.hasKey(key);
    }

    public long increment(String key, Duration duration) {
        Long value = redisTemplate.execute(ATOMIC_INCREMENT_WITH_TTL, java.util.List.of(key),
                String.valueOf(duration.toMillis()));
        return value == null ? 0 : value;
    }




    public boolean compareAndDeleteValues(String key, String expectedValue) {
        Long value = redisTemplate.execute(COMPARE_AND_DELETE, java.util.List.of(key), expectedValue);
        return Long.valueOf(1L).equals(value);
    }
}
