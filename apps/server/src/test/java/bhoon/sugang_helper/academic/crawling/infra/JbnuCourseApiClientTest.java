package bhoon.sugang_helper.academic.crawling.infra;

import static org.assertj.core.api.Assertions.assertThat;

import bhoon.sugang_helper.academic.crawling.application.JbnuCourseParser;
import bhoon.sugang_helper.academic.course.contract.ParsedCourseDto;
import io.github.cdimascio.dotenv.Dotenv;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

@Tag("manual")
class JbnuCourseApiClientTest {

    @Test
    @DisplayName("JUMP 세션을 획득하고 강좌 JSON을 가져옵니다")
    void fetchCourseData_realJumpCall() {
        // given
        Dotenv dotenv = Dotenv.configure().ignoreIfMissing().load();
        String apiUrl = firstNonBlank(dotenv.get("JBNU_API_URL"), System.getenv("JBNU_API_URL"),
                "https://jump.jbnu.ac.kr/SCH/SucrLessnSbjctInq/list.do");
        String bootstrapUrl = firstNonBlank(dotenv.get("JBNU_API_BOOTSTRAP_URL"),
                System.getenv("JBNU_API_BOOTSTRAP_URL"), "https://jump.jbnu.ac.kr/link?div=sucrPlan");

        JbnuCourseApiClient client = new JbnuCourseApiClient();
        ReflectionTestUtils.setField(client, "apiUrl", apiUrl);
        ReflectionTestUtils.setField(client, "bootstrapUrl", bootstrapUrl);
        ReflectionTestUtils.setField(client, "certDivision", "");
        ReflectionTestUtils.setField(client, "timeoutMs", 30_000);
        ReflectionTestUtils.setField(client, "maxRetries", 0);
        ReflectionTestUtils.setField(client, "maximumResponseBytes", 10 * 1024 * 1024);

        // when
        String result = client.fetchCourseData("2026", "U211600020");
        List<ParsedCourseDto> courses = new JbnuCourseParser().parseCourses(result, "2026", "U211600020");

        // then
        assertThat(result).contains("dsEstSbjList");
        assertThat(courses).isNotEmpty();
        System.out.printf("JUMP result: rows=%d, bytes=%d%n", courses.size(),
                result.getBytes(StandardCharsets.UTF_8).length);
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return "";
    }
}
