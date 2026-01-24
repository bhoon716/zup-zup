package bhoon.sugang_helper.domain.course.service;

import java.io.IOException;
import org.jsoup.Jsoup;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class JbnuCourseApiClient {

    @Value("${jbnu.api.url}")
    private String apiUrl;

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
                    <Parameter id="yy">2026</Parameter>
                    <Parameter id="shtm">U211600010</Parameter>
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

    public String fetchCourseDataXml() throws IOException {
        return Jsoup.connect(apiUrl)
                .header("Accept", "application/xml, text/xml, */*")
                .header("Accept-Language", "ko-KR,ko;q=0.9,en-US;q=0.8,en;q=0.7")
                .header("Content-Type", "text/xml")
                .header("Origin", "https://oasis.jbnu.ac.kr")
                .header("Referer", "https://oasis.jbnu.ac.kr/jbnu/sugang/sbjt/sbjt.html?param=KOR")
                .header("X-Requested-With", "XMLHttpRequest")
                .header("User-Agent",
                        "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/142.0.0.0 Whale/4.35.351.12 Safari/537.36")
                .requestBody(PAYLOAD_TEMPLATE)
                .timeout(30000)
                .method(org.jsoup.Connection.Method.POST)
                .ignoreContentType(true)
                .execute()
                .body();
    }
}
