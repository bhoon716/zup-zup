package bhoon.sugang_helper.domain.course.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("manual")
class JbnuCourseApiClientTest {

    @Test
    @DisplayName("Fetch Course Data - Real Network Call")
    void fetchCourseData_RealCall() throws IOException {
        // Given
        JbnuCourseApiClient client = new JbnuCourseApiClient();

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
