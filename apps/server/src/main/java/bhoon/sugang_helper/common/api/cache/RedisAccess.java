package bhoon.sugang_helper.common.api.cache;

import java.time.Duration;

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
