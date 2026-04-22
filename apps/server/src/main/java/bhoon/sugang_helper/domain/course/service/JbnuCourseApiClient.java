package bhoon.sugang_helper.domain.course.service;

import bhoon.sugang_helper.common.error.CustomException;
import bhoon.sugang_helper.common.error.ErrorCode;
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

    @Value("${jbnu.api.timeout-ms}")
    private int timeoutMs;

    @Value("${jbnu.api.max-retries}")
    private int maxRetries;

    @Value("${jbnu.api.retry-wait-ms:1000}")
    private int retryWaitMs;

    @Value("${jbnu.crawler.default-year}")
    private String defaultYear;

    @Value("${jbnu.crawler.default-semester}")
    private String defaultSemester;

    private static final String PAYLOAD_TEMPLATE = """
            <?xml version="1.0" encoding="UTF-8"?>
            <Root xmlns="http://www.nexacroplatform.com/platform/dataset">
                <Parameters>
                    <Parameter id="JSESSIONID" />
                    <Parameter id="gvYy">%s</Parameter>
                    <Parameter id="gvShtm">%s</Parameter>
                    <Parameter id="gvRechPrjtNo" />
                    <Parameter id="gvRechDutyr" />
                    <Parameter id="_fwb">20mzyFXlsiAAzAW4dFmDl2.1772273427418</Parameter>
                    <Parameter id="WMONID">zun7Ibxjuly</Parameter>
                    <Parameter id="JSESSIONIDSSO">QmLqIUuTLfXijjk9Lvam9DOMTcLjYxbrlkHJKnJz8PtcUNtKtZcFUJyZNIUvsvHd.amV1c19kb21haW4vc2VydmVyMV8z</Parameter>
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

    /**
     * 기본 서버 설정값(defaultYear, defaultSemester)을 사용하여 강의 데이터를 가져옵니다.
     */
    public String fetchCourseDataXml() {
        String year = (defaultYear == null || defaultYear.isBlank()) ? "2026" : defaultYear;
        String semester = (defaultSemester == null || defaultSemester.isBlank()) ? "U211600010" : defaultSemester;
        return fetchCourseDataXml(year, semester);
    }

    /**
     * 특정 년도와 학기를 지정하여 JBNU API 서버로부터 강의 데이터를 XML 형식으로 가져옵니다.
     */
    public String fetchCourseDataXml(String year, String semester) {
        String payload = PAYLOAD_TEMPLATE.formatted(year, semester, year, semester, year);
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
                        .maxBodySize(0)
                        .method(Connection.Method.POST)
                        .ignoreContentType(true)
                        .execute()
                        .body();
            } catch (Exception e) {
                retryCount++;
                log.warn("[API Client] Failed to request course data (attempt {}/{}): yy={}, shtm={}, reason={}",
                        retryCount, maxRetries + 1, year, semester, e.toString());

                if (retryCount > maxRetries) {
                    throw new CustomException(ErrorCode.CRAWLER_CONNECTION_ERROR,
                            "JBNU API 요청 최종 실패: " + e.toString());
                }

                waitBeforeRetry(retryCount);
            }
        }
    }

    /**
     * 재시도 전 일정 시간 대기합니다. (재시도 횟수에 따라 대기 시간 증가)
     *
     * @param retryCount 현재 재시도 횟수
     */
    private void waitBeforeRetry(int retryCount) {
        try {
            long waitTime = (long) retryWaitMs * retryCount;
            log.info("[API Client] Waiting {}ms before retry...", waitTime);
            Thread.sleep(waitTime);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            throw new CustomException(ErrorCode.INTERNAL_ERROR, "재시도 대기 중 프로세스가 중단되었습니다.");
        }
    }
}
