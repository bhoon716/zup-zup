package bhoon.sugang_helper.academic.crawling.client;

import static org.assertj.core.api.Assertions.assertThat;

import bhoon.sugang_helper.academic.crawling.service.CourseCrawlFetcher;
import bhoon.sugang_helper.academic.crawling.service.JbnuCourseParser;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class JbnuCourseApiClientSessionTest {

    private static final String YEAR = "2026";
    private static final String SEMESTER = "U211600020";
    private static final String SESSION_COOKIE_PREFIX = "JSESSIONID=jump-";
    private static final String FIRST_SESSION_COOKIE = SESSION_COOKIE_PREFIX + "1";
    private static final String SECOND_SESSION_COOKIE = SESSION_COOKIE_PREFIX + "2";

    private final AtomicInteger bootstrapRequests = new AtomicInteger();
    private final List<Integer> bootstrapPorts = new CopyOnWriteArrayList<>();
    private final List<Integer> coursePorts = new CopyOnWriteArrayList<>();
    private final List<String> courseCookies = new CopyOnWriteArrayList<>();
    private final List<String> courseRequests = new CopyOnWriteArrayList<>();

    private HttpServer server;
    private CourseCrawlFetcher fetcher;
    private volatile int failOnCourseRequest;

    @BeforeEach
    void setUp() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/bootstrap", exchange -> {
            try (exchange) {
                bootstrapPorts.add(exchange.getRemoteAddress().getPort());
                exchange.getResponseHeaders().add("Set-Cookie",
                        SESSION_COOKIE_PREFIX + bootstrapRequests.incrementAndGet() + "; Path=/");
                exchange.sendResponseHeaders(200, -1);
            }
        });
        server.createContext("/course", exchange -> {
            try (exchange) {
                coursePorts.add(exchange.getRemoteAddress().getPort());
                courseCookies.add(exchange.getRequestHeaders().getFirst("Cookie"));
                courseRequests.add(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
                int requestNumber = courseRequests.size();
                if (requestNumber == failOnCourseRequest) {
                    exchange.sendResponseHeaders(503, -1);
                    return;
                }
                byte[] response = """
                        {"dsEstSbjList":[{"SBJCT_CD":"COMP%s","DVCLS_NO":"01","SBJCT_NM":"자료구조"}]}
                        """.formatted(requestNumber).getBytes(StandardCharsets.UTF_8);
                exchange.sendResponseHeaders(200, response.length);
                exchange.getResponseBody().write(response);
            }
        });
        server.start();

        String baseUrl = "http://127.0.0.1:" + server.getAddress().getPort();
        JbnuCourseApiClient client = new JbnuCourseApiClient();
        ReflectionTestUtils.setField(client, "bootstrapUrl", baseUrl + "/bootstrap");
        ReflectionTestUtils.setField(client, "apiUrl", baseUrl + "/course");
        ReflectionTestUtils.setField(client, "timeoutMs", 5_000);
        ReflectionTestUtils.setField(client, "maxRetries", 1);
        ReflectionTestUtils.setField(client, "retryWaitMs", 0);
        ReflectionTestUtils.setField(client, "maximumResponseBytes", 10 * 1024 * 1024);
        fetcher = new CourseCrawlFetcher(client, new JbnuCourseParser());
        ReflectionTestUtils.setField(fetcher, "certDivisions", List.of("1", "2"));
        ReflectionTestUtils.setField(fetcher, "interRequestDelayMs", 0L);
    }

    @AfterEach
    void tearDown() {
        server.stop(0);
    }

    @Test
    @DisplayName("한 번의 강의 수집에서 연결과 쿠키를 공유하고 다음 수집은 새 세션을 사용한다")
    void reusesSessionWithinEachCrawl() {
        // when
        var first = fetcher.fetch(YEAR, SEMESTER);
        var second = fetcher.fetch(YEAR, SEMESTER);

        // then
        assertThat(first).hasSize(2);
        assertThat(second).hasSize(2);
        assertThat(bootstrapPorts).hasSize(2).doesNotHaveDuplicates();
        assertThat(coursePorts).containsExactly(bootstrapPorts.get(0), bootstrapPorts.get(0),
                bootstrapPorts.get(1), bootstrapPorts.get(1));
        assertThat(courseCookies).containsExactly(FIRST_SESSION_COOKIE, FIRST_SESSION_COOKIE,
                SECOND_SESSION_COOKIE, SECOND_SESSION_COOKIE);
    }

    @Test
    @DisplayName("공유 세션의 후속 분류 요청이 실패하면 새 세션으로 해당 분류만 재시도한다")
    void recreatesSessionBeforeRetryingFailedDivision() {
        // given
        failOnCourseRequest = 2;

        // when
        var courses = fetcher.fetch(YEAR, SEMESTER);

        // then
        assertThat(courses).hasSize(2);
        assertThat(bootstrapPorts).hasSize(2).doesNotHaveDuplicates();
        assertThat(coursePorts).containsExactly(bootstrapPorts.get(0), bootstrapPorts.get(0), bootstrapPorts.get(1));
        assertThat(courseCookies).containsExactly(FIRST_SESSION_COOKIE, FIRST_SESSION_COOKIE, SECOND_SESSION_COOKIE);
        assertThat(courseRequests.get(0)).isNotEqualTo(courseRequests.get(1));
        assertThat(courseRequests.get(1)).isEqualTo(courseRequests.get(2));
    }
}
