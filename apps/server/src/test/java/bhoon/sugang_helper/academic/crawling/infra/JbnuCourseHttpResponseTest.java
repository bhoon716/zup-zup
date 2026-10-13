package bhoon.sugang_helper.academic.crawling.infra;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.zip.GZIPOutputStream;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.client.MockClientHttpResponse;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestClientResponseException;

class JbnuCourseHttpResponseTest {

    private final JbnuCourseApiClient client = new JbnuCourseApiClient();

    @Test
    void readsJsonWithUnexpectedContentTypeAtTheSizeLimit() throws IOException {
        // given
        byte[] json = "{\"dsEstSbjList\":[]}".getBytes(StandardCharsets.UTF_8);
        MockClientHttpResponse response = new MockClientHttpResponse(json, HttpStatus.OK);
        response.getHeaders().set(HttpHeaders.CONTENT_TYPE, "text/html;charset=UTF-8");
        ReflectionTestUtils.setField(client, "maximumResponseBytes", json.length);

        // when
        byte[] actual = client.readResponse(response);

        // then
        assertThat(actual).isEqualTo(json);
    }

    @Test
    void rejectsAnOversizedResponseWhileReading() {
        // given
        MockClientHttpResponse response = new MockClientHttpResponse(new byte[11], HttpStatus.OK);
        ReflectionTestUtils.setField(client, "maximumResponseBytes", 10);

        // when & then
        assertThatThrownBy(() -> client.readResponse(response))
                .isInstanceOf(IOException.class)
                .hasMessageContaining("maximum size");
    }

    @Test
    void decodesGzipBeforeReturningTheBody() throws IOException {
        // given
        byte[] json = "{\"dsEstSbjList\":[]}".getBytes(StandardCharsets.UTF_8);
        MockClientHttpResponse response = gzipResponse(json);
        ReflectionTestUtils.setField(client, "maximumResponseBytes", json.length);

        // when
        byte[] actual = client.readResponse(response);

        // then
        assertThat(actual).isEqualTo(json);
    }

    @Test
    void limitsTheDecompressedSizeOfGzipResponses() throws IOException {
        // given
        MockClientHttpResponse response = gzipResponse(new byte[1_000]);
        ReflectionTestUtils.setField(client, "maximumResponseBytes", 100);

        // when & then
        assertThatThrownBy(() -> client.readResponse(response))
                .isInstanceOf(IOException.class)
                .hasMessageContaining("maximum size");
    }

    @Test
    void preservesAnHttpFailureStatusWithoutKeepingItsBody() {
        // given
        MockClientHttpResponse response = new MockClientHttpResponse(
                "upstream secret".getBytes(StandardCharsets.UTF_8), HttpStatus.SERVICE_UNAVAILABLE);

        // when & then
        assertThatThrownBy(() -> client.readResponse(response))
                .isInstanceOfSatisfying(RestClientResponseException.class, exception -> {
                    assertThat(exception.getStatusCode().value()).isEqualTo(503);
                    assertThat(exception.getResponseBodyAsByteArray()).isEmpty();
                    assertThat(exception.getMessage()).doesNotContain("upstream secret");
                    assertThat(JbnuCourseApiClient.isTransientFailure(exception)).isTrue();
                });
    }

    private MockClientHttpResponse gzipResponse(byte[] body) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try (GZIPOutputStream gzip = new GZIPOutputStream(output)) {
            gzip.write(body);
        }
        MockClientHttpResponse response = new MockClientHttpResponse(output.toByteArray(), HttpStatus.OK);
        response.getHeaders().set(HttpHeaders.CONTENT_ENCODING, "gzip");
        return response;
    }
}
