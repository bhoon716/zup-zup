package bhoon.sugang_helper.crawling.infra;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import org.junit.jupiter.api.Test;

class BoundedInputStreamTest {

    @Test
    void responseLargerThanTheConfiguredLimitIsRejected() {
        BoundedInputStream input = new BoundedInputStream(new ByteArrayInputStream(new byte[11]), 10);

        assertThatThrownBy(input::readAllBytes)
                .isInstanceOf(IOException.class)
                .hasMessageContaining("maximum size");
    }
}
