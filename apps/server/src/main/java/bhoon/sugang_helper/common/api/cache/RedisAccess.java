package bhoon.sugang_helper.common.api.cache;

import java.time.Duration;

/** 다른 모듈이 사용하는 RedisAccess 계약. */
public interface RedisAccess {

    void setValues(String key, String data);

    void setValues(String key, String data, Duration duration);

    boolean setValuesIfAbsent(String key, String data, Duration duration);

    String getValues(String key);

    void deleteValues(String key);

    boolean hasKey(String key);

    long increment(String key, Duration duration);

    boolean compareAndDeleteValues(String key, String expectedValue);
}
