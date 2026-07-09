package bhoon.sugang_helper.crawling.infra;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.cdimascio.dotenv.Dotenv;
import java.io.IOException;
import java.util.Map;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

@Tag("manual")
class JbnuCourseApiClientTest {

    private static final String JBNU_API_URL_ENV = "JBNU_API_URL";

    @Test
    @DisplayName("실제 강좌 데이터 가져오기")
    void fetchCourseData_RealCall() throws IOException {
        // Given
        Dotenv dotenv = Dotenv.configure().ignoreIfMissing().load();
        String realApiUrl = dotenv.get(JBNU_API_URL_ENV);

        if (realApiUrl == null || realApiUrl.isBlank()) {
            realApiUrl = System.getenv(JBNU_API_URL_ENV);
        }

        if (realApiUrl == null || realApiUrl.isBlank()) {
            throw new IllegalStateException(
                    JBNU_API_URL_ENV + " environment variable is required for manual tests.\n" +
                            "Please check your .env file or system environment variables.");
        }

        JbnuCourseApiClient client = new JbnuCourseApiClient();
        ReflectionTestUtils.setField(client, "apiUrl", realApiUrl);

        // When
        String result = client.fetchCourseDataXml("2026", "U211600010");

        // Then
        assertThat(result).isNotNull();
        System.out.println(
                "Response excerpt: "
                        + (result.length() > 500 ? result.substring(0, 500) : result));
        assertThat(result).as("Server Response Content: %s", result)
                .doesNotContain("MSG_F001")
                .contains("Dataset")
                .contains("SBJTCD");
    }

    @Test
    @DisplayName("동적 세션 획득 및 API 호출 테스트")
    void testDynamicSessionFetch() throws IOException {
        // 1. Get cookies from login page
        Connection.Response loginRes = Jsoup.connect("https://oasis.jbnu.ac.kr/com/login.do")
                .method(Connection.Method.GET)
                .execute();

        Map<String, String> cookies = loginRes.cookies();
        String wmonid = cookies.get("WMONID");
        String jsessionidsso = cookies.get("JSESSIONIDSSO");

        System.out.println("Fetched WMONID: " + wmonid);
        System.out.println("Fetched JSESSIONIDSSO: " + jsessionidsso);

        assertThat(wmonid).isNotNull();
        assertThat(jsessionidsso).isNotNull();

        // 2. Perform API call with these cookies in Payload
        Dotenv dotenv = Dotenv.configure().ignoreIfMissing().load();
        String realApiUrl = dotenv.get(JBNU_API_URL_ENV);
        if (realApiUrl == null || realApiUrl.isBlank()) {
            realApiUrl = System.getenv(JBNU_API_URL_ENV);
        }
        if (realApiUrl == null || realApiUrl.isBlank()) {
            realApiUrl = "https://oasis.jbnu.ac.kr/uni/uni/cour/less/findLessSubjtTblInq.action";
        }

        String payloadTemplate = """
                <?xml version="1.0" encoding="UTF-8"?>
                <Root xmlns="http://www.nexacroplatform.com/platform/dataset">
                    <Parameters>
                        <Parameter id="JSESSIONID" />
                        <Parameter id="gvYy">%s</Parameter>
                        <Parameter id="gvShtm">%s</Parameter>
                        <Parameter id="gvRechPrjtNo" />
                        <Parameter id="gvRechDutyr" />
                        <Parameter id="_fwb" />
                        <Parameter id="WMONID">%s</Parameter>
                        <Parameter id="JSESSIONIDSSO">%s</Parameter>
                        <Parameter id="yy">%s</Parameter>
                        <Parameter id="shtm">%s</Parameter>
                        <Parameter id="fg" />
                        <Parameter id="value1" />
                        <Parameter id="value2" />
                        <Parameter id="value3" />
                        <Parameter id="sbjtNm" />
                        <Parameter id="profNm" />
                        <Parameter id="openLectFg" />
                        <Parameter id="entrYy">%s</Parameter>
                        <Parameter id="sType">EXT1</Parameter>
                        <Parameter id="lang">K</Parameter>
                        <Parameter id="ltLangFg">N</Parameter>
                    </Parameters>
                </Root>
                """;

        String year = "2026";
        String semester = "U211600010";
        String payload = payloadTemplate.formatted(year, semester, wmonid, jsessionidsso, year, semester, year);

        String result = Jsoup.connect(realApiUrl)
                .header("Accept", "application/xml, text/xml, */*")
                .header("Accept-Language", "ko-KR,ko;q=0.9,en-US;q=0.8,en;q=0.7")
                .header("Content-Type", "text/xml")
                .header("Origin", "https://oasis.jbnu.ac.kr")
                .header("Referer", "https://oasis.jbnu.ac.kr/jbnu/sugang/sbjt/sbjt.html?param=KOR")
                .header("X-Requested-With", "XMLHttpRequest")
                .header("User-Agent",
                        "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/142.0.0.0 Whale/4.35.351.12 Safari/537.36")
                .requestBody(payload)
                .timeout(30000)
                .maxBodySize(0)
                .method(Connection.Method.POST)
                .ignoreContentType(true)
                .execute()
                .body();

        assertThat(result).isNotBlank();
        System.out.println("Result length: " + result.length());
        assertThat(result).doesNotContain("MSG_F001")
                .contains("Dataset")
                .contains("SBJTCD");
    }
}
