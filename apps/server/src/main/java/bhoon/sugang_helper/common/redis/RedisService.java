package bhoon.sugang_helper.common.redis;

import java.time.Duration;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RedisService {

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
}
