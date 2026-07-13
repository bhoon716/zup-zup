package bhoon.sugang_helper.common.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import io.lettuce.core.ClientOptions;
import io.lettuce.core.protocol.RedisCommand;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;

class RedisConfigTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(RedisAutoConfiguration.class))
            .withUserConfiguration(RedisConfig.class)
            .withPropertyValues(
                    "spring.data.redis.host=redis",
                    "spring.data.redis.port=6379",
                    "spring.data.redis.password=test-password",
                    "spring.data.redis.timeout=2s",
                    "spring.data.redis.connect-timeout=2s");

    @Test
    void redisClientFailsFastWhileReconnectingAfterAnOutage() {
        contextRunner.run(context -> {
            LettuceConnectionFactory factory = context.getBean(LettuceConnectionFactory.class);
            var clientConfiguration = factory.getClientConfiguration();
            ClientOptions options = clientConfiguration.getClientOptions().orElseThrow();

            assertThat(clientConfiguration.getCommandTimeout()).isEqualTo(Duration.ofSeconds(2));
            assertThat(options.getSocketOptions().getConnectTimeout()).isEqualTo(Duration.ofSeconds(2));
            assertThat(options.getTimeoutOptions().isTimeoutCommands()).isTrue();
            assertThat(options.isAutoReconnect()).isTrue();
            assertThat(options.getDisconnectedBehavior())
                    .isEqualTo(ClientOptions.DisconnectedBehavior.REJECT_COMMANDS);
            assertThat(options.getReplayFilter().test(mock(RedisCommand.class))).isTrue();
        });
    }
}
