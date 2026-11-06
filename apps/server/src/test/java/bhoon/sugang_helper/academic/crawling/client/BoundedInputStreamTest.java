package bhoon.sugang_helper.academic.crawling.client;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class BoundedInputStreamTest {

    @Test
    @DisplayName("설정된 크기를 넘는 응답 스트림을 거부한다")
    void responseLargerThanTheConfiguredLimitIsRejected() {
        // given
        BoundedInputStream input = new BoundedInputStream(new ByteArrayInputStream(new byte[11]), 10);

        // when & then
        assertThatThrownBy(input::readAllBytes)
                .isInstanceOf(IOException.class)
                .hasMessageContaining("maximum size");
    }
}
