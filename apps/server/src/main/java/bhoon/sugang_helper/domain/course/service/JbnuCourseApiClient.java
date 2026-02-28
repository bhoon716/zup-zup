package bhoon.sugang_helper.domain.course.service;

import java.io.IOException;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class JbnuCourseApiClient {

    @Value("${jbnu.api.url}")
    private String apiUrl;

    @Value("${jbnu.api.timeout-ms:30000}")
    private int timeoutMs;

    @Value("${jbnu.api.max-retries:1}")
    private int maxRetries;

    @Value("${jbnu.crawler.default-year}")
    private String defaultYear;

    @Value("${jbnu.crawler.default-semester}")
    private String defaultSemester;

    private static final String PAYLOAD_TEMPLATE = """
            <?xml version="1.0" encoding="UTF-8"?>
            <Root xmlns="http://www.nexacroplatform.com/platform/dataset">
                <Parameters>
                    <Parameter id="JSESSIONID" />
                    <Parameter id="gvYy">2017</Parameter>
                    <Parameter id="gvShtm">U211600010</Parameter>
                    <Parameter id="gvRechPrjtNo" />
                    <Parameter id="gvRechDutyr" />
                    <Parameter id="_fwb">48AUDbfWcQlwqD8plRUMBF.1763450651870</Parameter>
                    <Parameter id="WMONID">zaIN2L1Cwla</Parameter>
                    <Parameter id="yy">%s</Parameter>
                    <Parameter id="shtm">%s</Parameter>
                    <Parameter id="fg" />
                    <Parameter id="value1" />
                    <Parameter id="value2" />
                    <Parameter id="value3" />
                    <Parameter id="sbjtNm" />
                    <Parameter id="profNm" />
                    <Parameter id="openLectFg" />
                    <Parameter id="entrYy">2017</Parameter>
                    <Parameter id="sType">EXT1</Parameter>
                    <Parameter id="lang">K</Parameter>
                    <Parameter id="ltLangFg">N</Parameter>
                </Parameters>
            </Root>
            """;

    /**
     * 기본 서버 설정값(defaultYear, defaultSemester)을 사용하여 강의 데이터를 가져옵니다.
     */
    public String fetchCourseDataXml() throws IOException {
        String year = (defaultYear == null || defaultYear.isBlank()) ? "2026" : defaultYear;
        String semester = (defaultSemester == null || defaultSemester.isBlank()) ? "U211600010" : defaultSemester;
        return fetchCourseDataXml(year, semester);
    }

    /**
     * 특정 년도와 학기를 지정하여 JBNU API 서버로부터 강의 데이터를 XML 형식으로 가져옵니다.
     */
    public String fetchCourseDataXml(String year, String semester) throws IOException {
        String payload = PAYLOAD_TEMPLATE.formatted(year, semester);
        int retryCount = 0;
        while (true) {
            try {
                return Jsoup.connect(apiUrl)
                        .header("Accept", "application/xml, text/xml, */*")
                        .header("Accept-Language", "ko-KR,ko;q=0.9,en-US;q=0.8,en;q=0.7")
                        .header("Content-Type", "text/xml")
                        .header("Origin", "https://oasis.jbnu.ac.kr")
                        .header("Referer", "https://oasis.jbnu.ac.kr/jbnu/sugang/sbjt/sbjt.html?param=KOR")
                        .header("X-Requested-With", "XMLHttpRequest")
                        .header("User-Agent",
                                "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/142.0.0.0 Whale/4.35.351.12 Safari/537.36")
                        .requestBody(payload)
                        .timeout(timeoutMs)
                        .maxBodySize(0) // 무제한 수신
                        .method(Connection.Method.POST)
                        .ignoreContentType(true)
                        .execute()
                        .body();
            } catch (IOException e) {
                retryCount++;
                log.warn("[API 클라이언트] 강의 데이터 요청 실패 (시도 {}/{}): yy={}, shtm={}, reason={}",
                        retryCount, maxRetries + 1, year, semester, e.getMessage());
                if (retryCount > maxRetries) {
                    throw e;
                }
                try {
                    Thread.sleep(1000); // 1초 대기 후 재시도
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new IOException("재시도 대기 중 인터럽트 발생", ie);
                }
            }
        }
    }
}
