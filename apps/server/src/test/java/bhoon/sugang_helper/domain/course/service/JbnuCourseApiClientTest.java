package bhoon.sugang_helper.domain.course.service;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.cdimascio.dotenv.Dotenv;
import java.io.IOException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

@Tag("manual")
class JbnuCourseApiClientTest {

    @Test
    @DisplayName("실제 강좌 데이터 가져오기")
    void fetchCourseData_RealCall() throws IOException {
        // Given
        Dotenv dotenv = Dotenv.configure().ignoreIfMissing().load();
        String realApiUrl = dotenv.get("JBNU_API_URL");

        if (realApiUrl == null || realApiUrl.isBlank()) {
            realApiUrl = System.getenv("JBNU_API_URL");
        }

        if (realApiUrl == null || realApiUrl.isBlank()) {
            throw new IllegalStateException(
                    "JBNU_API_URL environment variable is required for manual tests.\n" +
                            "Please check your .env file or system environment variables.");
        }

        JbnuCourseApiClient client = new JbnuCourseApiClient();
        ReflectionTestUtils.setField(client, "apiUrl", realApiUrl);

        // When
        String result = client.fetchCourseDataXml();

        // Then
        System.out.println(
                "Response excerpt: "
                        + (result != null && result.length() > 500 ? result.substring(0, 500)
                        : result));

        assertThat(result).isNotNull();
        assertThat(result).as("Server Response Content: %s", result)
                .doesNotContain("MSG_F001")
                .contains("Dataset")
                .contains("SBJTCD");
    }
}
