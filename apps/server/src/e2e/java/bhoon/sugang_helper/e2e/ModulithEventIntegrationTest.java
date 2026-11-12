package bhoon.sugang_helper.e2e;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import bhoon.sugang_helper.SugangHelperApplication;
import bhoon.sugang_helper.academic.course.dto.external.ParsedCourse;
import bhoon.sugang_helper.academic.course.entity.Course;
import bhoon.sugang_helper.academic.course.repository.CourseRepository;
import bhoon.sugang_helper.academic.crawling.client.CourseApiClient;
import bhoon.sugang_helper.academic.crawling.service.CourseCrawlCompletionService;
import bhoon.sugang_helper.academic.events.SeatOpenedEvent;
import bhoon.sugang_helper.audit.repository.AdminActionLogRepository;
import bhoon.sugang_helper.common.alert.SlackAlertService;
import bhoon.sugang_helper.common.api.error.ErrorCode;
import bhoon.sugang_helper.identity.auth.service.SessionPrincipal;
import bhoon.sugang_helper.identity.events.UserWithdrawnEvent;
import bhoon.sugang_helper.identity.user.entity.User;
import bhoon.sugang_helper.identity.user.entity.UserDevice;
import bhoon.sugang_helper.identity.user.enums.Role;
import bhoon.sugang_helper.identity.user.repository.UserDeviceRepository;
import bhoon.sugang_helper.identity.user.repository.UserRepository;
import bhoon.sugang_helper.identity.user.service.UserService;
import bhoon.sugang_helper.planning.wishlist.entity.Wishlist;
import bhoon.sugang_helper.planning.wishlist.repository.WishlistRepository;
import bhoon.sugang_helper.seatalert.api.NotificationChannel;
import bhoon.sugang_helper.seatalert.notification.service.InboxChannelDeliveryWorker;
import bhoon.sugang_helper.seatalert.notification.client.DiscordNotificationSender;
import bhoon.sugang_helper.seatalert.notification.client.EmailNotificationSender;
import bhoon.sugang_helper.seatalert.notification.client.WebPushNotificationSender;
import bhoon.sugang_helper.seatalert.notification.entity.SeatNotificationDelivery;
import bhoon.sugang_helper.seatalert.notification.entity.SeatNotificationOutbox;
import bhoon.sugang_helper.seatalert.notification.exception.NotificationProviderException;
import bhoon.sugang_helper.seatalert.notification.repository.SeatNotificationDeliveryRepository;
import bhoon.sugang_helper.seatalert.notification.repository.SeatNotificationOutboxRepository;
import bhoon.sugang_helper.seatalert.notification.scheduler.SeatNotificationOutboxWorker;
import bhoon.sugang_helper.seatalert.notification.service.SeatNotificationDeliveryStateService;
import bhoon.sugang_helper.seatalert.notification.service.SeatNotificationOutboxProcessor;
import bhoon.sugang_helper.seatalert.notification.service.UserNotificationTestService;
import bhoon.sugang_helper.seatalert.subscription.entity.Subscription;
import bhoon.sugang_helper.seatalert.subscription.repository.SubscriptionRepository;
import bhoon.sugang_helper.support.feedback.entity.Feedback;
import bhoon.sugang_helper.support.feedback.enums.FeedbackType;
import bhoon.sugang_helper.support.feedback.repository.FeedbackRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import javax.sql.DataSource;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestComponent;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@Tag("modulith-e2e")
@Testcontainers(disabledWithoutDocker = false)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@SpringBootTest(classes = {SugangHelperApplication.class, ModulithEventIntegrationTest.Fixture.class},
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "spring.config.location=classpath:/application.yml",
        "spring.profiles.active=modulith-e2e",
        "server.address=127.0.0.1",
        "logging.structured.json.add.environment=E2E",
        "spring.datasource.url=jdbc:h2:mem:modulith-e2e;MODE=MySQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.open-in-view=false",
        "spring.flyway.enabled=false",
        "spring.data.redis.password=",
        "spring.data.redis.repositories.enabled=false",
        "spring.cloud.aws.ses.enabled=false",
        "management.health.mail.enabled=false",
        "app.notification.channels.email=true",
        "spring.cloud.aws.region.static=us-east-1",
        "spring.cloud.aws.credentials.access-key=isolated-e2e",
        "spring.cloud.aws.credentials.secret-key=isolated-e2e",
        "spring.mail.from=module-e2e@example.invalid",
        "spring.mail.from-name=Module E2E",
        "app.cors.allowed-origins=http://localhost:3000",
        "app.oauth2.success-redirect-uri=http://localhost:3000",
        "app.oauth2.failure-redirect-uri=http://localhost:3000/login",
        "app.oauth2.authorized-redirect-uri=http://localhost:3000",
        "app.discord.client-id=isolated-e2e",
        "app.discord.client-secret=isolated-e2e",
        "app.discord.redirect-uri=http://127.0.0.1/disabled-discord",
        "app.admin-audit.fingerprint-secret=isolated-modulith-e2e-fingerprint-key",
        "app.admin-audit.retention-cron=-",
        "app.notification.outbox.dlq-retention-cron=-",
        "app.notification.outbox.poll-ms=3600000",
        "file.upload.orphan-cleanup-cron=-",
        "file.upload.dir=${java.io.tmpdir}/zup-modulith-e2e-unused",
        "jbnu.crawler.run-on-startup=false",
        "jbnu.crawler.cron=-",
        "jbnu.crawler.default-year=2026",
        "jbnu.crawler.default-semester=U211600010",
        "jbnu.api.cert-divisions=6",
        "jbnu.api.inter-request-delay-ms=0",
        "spring.security.oauth2.client.registration.google.client-id=isolated-e2e",
        "spring.security.oauth2.client.registration.google.client-secret=isolated-e2e",
        "spring.security.oauth2.client.registration.google.scope=profile,email",
        "spring.security.oauth2.client.registration.google.authorization-grant-type=authorization_code",
        "spring.security.oauth2.client.provider.google.authorization-uri=http://127.0.0.1/disabled-oauth",
        "spring.security.oauth2.client.provider.google.token-uri=http://127.0.0.1/disabled-token",
        "spring.security.oauth2.client.provider.google.user-info-uri=http://127.0.0.1/disabled-userinfo",
        "spring.security.oauth2.client.provider.google.user-name-attribute=sub"
})
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
class ModulithEventIntegrationTest {

    private static final String PREFIX = "/oauth2/modulith-e2e";
    private static final String COURSE_KEY = "modulith-e2e-course";
    private static final String EMAIL = "module-e2e@example.invalid";
    private static final String OTHER_EMAIL = "other-inbox@example.invalid";
    private static final String INBOX = "/api/v1/notifications/inbox";
    private static final String ADMIN_ANNOUNCEMENTS = "/api/v1/admin/announcements";
    private static final String ADMIN_FEEDBACKS = "/api/v1/admin/feedbacks";
    private static final String REPLY_NOTIFICATION_TITLE = "문의에 답변이 등록되었습니다";
    private static final String INBOX_OCCURRED_AT = "2024-03-04T10:00:00";

    @Container
    private static final GenericContainer<?> REDIS =
            new GenericContainer<>(DockerImageName.parse("redis:6-alpine")).withExposedPorts(6379);

    @DynamicPropertySource
    static void isolatedRedis(DynamicPropertyRegistry properties) {
        properties.add("spring.data.redis.host", REDIS::getHost);
        properties.add("spring.data.redis.port", () -> REDIS.getMappedPort(6379));
    }

    @LocalServerPort private int port;

    @Autowired private ObjectMapper mapper;

    @Autowired private PlatformTransactionManager transactionManager;

    @MockitoBean private CourseApiClient courseApi;

    @MockitoBean private JavaMailSender mail;

    @MockitoBean private SlackAlertService slack;

    @MockitoBean private WebPushNotificationSender webPush;

    @MockitoBean private DiscordNotificationSender discord;

    @MockitoBean private EmailNotificationSender emailSender;

    @MockitoBean private SeatNotificationOutboxWorker worker;

    @MockitoSpyBean private SeatNotificationOutboxRepository outbox;

    @MockitoSpyBean private FeedbackRepository feedback;

    @MockitoSpyBean private AdminActionLogRepository audits;

    @MockitoSpyBean private UserNotificationTestService notificationUseCase;

    private HttpClient browser;
    private String csrf;
    private boolean simulatedProviderCall;

    @BeforeEach
    void resetThroughHttp() throws Exception {
        assertThat(transactionManager).isInstanceOf(JpaTransactionManager.class);
        simulatedProviderCall = false;
        browser = HttpClient.newBuilder().cookieHandler(new CookieManager(null, CookiePolicy.ACCEPT_ALL))
                .connectTimeout(Duration.ofSeconds(5)).build();
        HttpResponse<String> token = get("/api/auth/csrf");
        assertThat(token.statusCode()).isEqualTo(200);
        csrf = mapper.readTree(token.body()).path("token").asText();
        assertThat(csrf).isNotBlank();
        assertThat(post("/reset").statusCode()).isEqualTo(200);
    }

    @AfterEach
    void externalTransportsWereNotCalled() {
        verifyNoInteractions(courseApi, mail);
        if (!simulatedProviderCall) {
            verifyNoInteractions(webPush, discord, emailSender);
        }
    }

    @Test
    @ExtendWith(OutputCaptureExtension.class)
    @DisplayName("실제 HTTP 오류 로그는 마스킹과 검색 필드를 유지한 한 행 JSON으로 출력된다")
    void httpFailureProducesValidRedactedJsonLogs(CapturedOutput output) throws Exception {
        // given
        RuntimeException failure = new UnsupportedOperationException("token=private-runtime-token",
                new IllegalArgumentException("cookie=private-runtime-cookie"));
        failure.addSuppressed(new IOException("email=private-runtime@example.invalid"));
        doThrow(failure).when(outbox).save(any());

        // when
        HttpResponse<String> response = post("/crawl?mode=commit&token=private-query-token");
        List<JsonNode> records = jsonLogLines(output.getOut());

        // then
        assertThat(response.statusCode()).isEqualTo(500);
        assertThat(records).isNotEmpty().allSatisfy(record -> {
            assertThat(record.isObject()).isTrue();
            assertThat(record.path("@timestamp").asText()).isNotBlank();
            assertThat(record.path("level").asText()).isNotBlank();
            assertThat(record.path("message").asText()).isNotBlank();
            assertThat(record.path("environment").asText()).isEqualTo("E2E");
        });
        String correlationId = response.headers().firstValue("X-Error-Id").orElseThrow();
        JsonNode error = records.stream()
                .filter(record -> record.path("event").asText().equals("api_error"))
                .filter(record -> record.path("correlationId").asText().equals(correlationId))
                .findFirst().orElseThrow();
        assertThat(error.path("level").asText()).isEqualTo("ERROR");
        assertThat(error.path("errorCode").asText()).isEqualTo(ErrorCode.INTERNAL_ERROR.getCode());
        assertThat(error.path("exceptionType").asText()).isEqualTo("UnsupportedOperationException");
        assertThat(error.path("rootCauseType").asText()).isEqualTo("IllegalArgumentException");
        assertThat(error.path("status").asInt()).isEqualTo(500);
        assertThat(error.path("method").asText()).isEqualTo("POST");
        assertThat(error.path("path").asText()).isEqualTo(PREFIX + "/crawl");
        assertThat(error.path("stackTraceIncluded").asBoolean()).isTrue();
        assertThat(error.path("stack_trace").asText()).contains("Caused by:", "Suppressed:", "원문 메시지 생략");
        assertThat(output.getOut()).doesNotContain("private-runtime-token", "private-runtime-cookie",
                "private-runtime@example.invalid", "private-query-token");
    }

    private List<JsonNode> jsonLogLines(String output) throws IOException {
        List<JsonNode> records = new ArrayList<>();
        for (String line : output.lines().filter(value -> !value.isBlank()).toList()) {
            records.add(mapper.readTree(line));
        }
        return records;
    }

    @Test
    @DisplayName("전체 앱이 좌석 리스너를 발견하고 같은 결과의 Outbox를 한 번만 저장한다")
    void fullApplicationDiscoversOutboxListenerAndCommitsOnce() throws Exception {
        // when & then
        assertThat(post("/crawl?mode=commit").statusCode()).isEqualTo(200);

        JsonNode state = state();
        assertThat(state.path("current").asInt()).isEqualTo(59);
        assertThat(state.path("history").asInt()).isEqualTo(1);
        assertThat(state.path("outbox").asInt()).isEqualTo(1);
        assertThat(state.path("success").asInt()).isEqualTo(1);
        assertThat(post("/crawl?mode=commit").statusCode()).isEqualTo(200);
        assertThat(state().path("outbox").asInt()).isEqualTo(1);

        HttpResponse<String> dispatch = post("/dispatch");
        assertThat(dispatch.statusCode()).isEqualTo(200);
        assertThat(mapper.readTree(dispatch.body()).path("claimed").asInt()).isZero();
        loginFixture();
        assertThat(firstInboxItem("").path("courseKey").asText()).isEqualTo(COURSE_KEY);
        assertThat(inbox("").path("totalElements").asInt()).isEqualTo(1);
    }

    @Test
    @DisplayName("호출자 실패는 강의·좌석 이력·성공 상태·Outbox를 함께 롤백한다")
    void callerFailureRollsBackCourseHistorySuccessAndOutbox() throws Exception {
        // when & then
        assertThat(post("/crawl?mode=rollback").statusCode()).isEqualTo(500);

        assertCatalogRolledBack(state());
    }

    @Test
    @DisplayName("Outbox 저장 실패는 앞서 저장한 청크도 롤백한다")
    void outboxFailureRollsBackAnEarlierWrittenChunk() throws Exception {
        // given
        doThrow(new IllegalStateException("forced isolated outbox failure")).when(outbox).save(any());

        // when & then
        assertThat(post("/crawl?mode=chunk-failure").statusCode()).isEqualTo(500);

        JsonNode state = state();
        assertCatalogRolledBack(state);
        assertThat(state.path("courses").asInt()).isEqualTo(1);
    }

    @Test
    @DisplayName("탈퇴 리스너는 구독·문의·알림함 항목을 숨긴다")
    void fullApplicationDiscoversWithdrawalListeners() throws Exception {
        // given
        seedInbox(1);
        loginFixture();
        assertThat(inbox("").path("totalElements").asInt()).isEqualTo(1);

        // when
        assertThat(post("/withdraw?rollback=false").statusCode()).isEqualTo(200);

        JsonNode state = state();
        assertThat(state.path("withdrawn").asInt()).isEqualTo(1);
        assertThat(state.path("activeSubscriptions").asInt()).isEqualTo(1);
        assertThat(state.path("activeFeedback").asInt()).isZero();
        assertThat(state.path("activeInbox").asInt()).isEqualTo(1);
        assertThat(state.path("devices").asInt()).isZero();
        assertThat(get(INBOX).statusCode()).isEqualTo(401);
        loginFixture("?other=true");
        assertThat(inbox("").path("totalElements").asInt()).isEqualTo(1);
    }

    @Test
    @DisplayName("호출자 실패는 모든 탈퇴 리스너의 DB 변경을 롤백한다")
    void callerFailureRollsBackAllWithdrawalListeners() throws Exception {
        // given
        seedInbox(1);
        loginFixture();

        // when & then
        assertThat(post("/withdraw?rollback=true").statusCode()).isEqualTo(500);
        assertWithdrawalRolledBack(state(), 2);
        loginFixture();
        assertThat(inbox("").path("totalElements").asInt()).isEqualTo(1);
    }

    @Test
    @DisplayName("탈퇴 리스너 실패가 HTTP 오류로 전달되고 계정 변경도 롤백된다")
    void withdrawalListenerFailureReachesHttpAndRollsBackAccount() throws Exception {
        // given
        doThrow(new IllegalStateException("forced isolated feedback failure"))
                .when(feedback).findAllByUserId(any());

        // when & then
        assertThat(post("/withdraw?rollback=false").statusCode()).isEqualTo(500);

        assertWithdrawalRolledBack(state(), 1);
    }

    @Test
    @DisplayName("자동 등록된 리스너는 트랜잭션 밖의 이벤트 처리를 거부한다")
    void automaticallyRegisteredListenersRejectEventsOutsideATransaction() throws Exception {
        // when & then
        assertThat(post("/outside?event=seat").statusCode()).isEqualTo(500);
        assertThat(post("/outside?event=withdrawal").statusCode()).isEqualTo(500);
        assertCatalogRolledBack(state());
        assertWithdrawalRolledBack(state(), 1);
    }

    @Test
    @DisplayName("인기순 정렬과 페이지 처리는 기존 검색 결과 순서를 유지한다")
    void popularitySortingAndPaginationKeepTheExistingQuerySemantics() throws Exception {
        // when & then
        assertThat(post("/seed-search").statusCode()).isEqualTo(200);

        JsonNode first = search(Map.of("sortBy", "popular", "sortOrder", "desc"), 0);
        JsonNode second = search(Map.of("sortBy", "popular", "sortOrder", "desc"), 1);
        JsonNode ascending = search(Map.of("sortBy", "popular", "sortOrder", "asc"), 0);

        assertThat(first.path("content").size()).isEqualTo(30);
        assertThat(second.path("content").size()).isEqualTo(6);
        assertThat(first.path("last").asBoolean()).isFalse();
        assertThat(second.path("last").asBoolean()).isTrue();
        assertThat(first.path("content").get(0).path("courseKey").asText()).isEqualTo("modulith-course-c");
        assertThat(first.path("content").get(1).path("courseKey").asText()).isEqualTo("modulith-course-b");
        List<String> keys = new ArrayList<>();
        first.path("content").forEach(course -> keys.add(course.path("courseKey").asText()));
        second.path("content").forEach(course -> keys.add(course.path("courseKey").asText()));
        assertThat(keys).hasSize(36).doesNotHaveDuplicates();
        assertThat(ascending.path("content").get(0).path("courseKey").asText()).isEqualTo(COURSE_KEY);
    }

    @Test
    @DisplayName("찜 검색은 모듈 계약을 거쳐 인증된 사용자의 목록만 조회한다")
    void wishedOnlySearchUsesTheAuthenticatedUserAcrossTheModuleContract() throws Exception {
        // when & then
        assertThat(post("/seed-search").statusCode()).isEqualTo(200);
        loginFixture();

        JsonNode result = search(Map.of("isWishedOnly", true, "userId", 999999L), 0);

        assertThat(result.path("content").size()).isEqualTo(1);
        assertThat(result.path("content").get(0).path("courseKey").asText()).isEqualTo("modulith-course-b");
    }

    @Test
    @DisplayName("발송 채널을 끈 상태에서 오래된 빈자리 알림을 정해진 순서와 크기로 조회하고 기호를 검색한다")
    void inboxPersistsSeatOpeningsAndPagesByOccurrenceAndId() throws Exception {
        // given
        seedInbox(32);
        assertThat(get(INBOX).statusCode()).isEqualTo(401);
        loginFixture();

        // when
        JsonNode first = inbox("?size=1&sort=id,asc");
        JsonNode second = inbox("?page=1");
        JsonNode literalMatch = inbox("?courseName=sql%25_%21&read=UNREAD");

        // then
        assertThat(first.path("number").asInt()).isZero();
        assertThat(second.path("number").asInt()).isEqualTo(1);
        assertThat(first.path("totalElements").asInt()).isEqualTo(32);
        assertThat(first.path("totalPages").asInt()).isEqualTo(2);
        assertThat(first.path("content").size()).isEqualTo(30);
        assertThat(second.path("content").size()).isEqualTo(2);
        List<Long> ids = new ArrayList<>();
        first.path("content").forEach(item -> ids.add(item.path("id").asLong()));
        second.path("content").forEach(item -> ids.add(item.path("id").asLong()));
        assertThat(ids).doesNotHaveDuplicates().isSortedAccordingTo(Comparator.reverseOrder());
        assertThat(literalMatch.path("totalElements").asInt()).isEqualTo(1);
        JsonNode item = literalMatch.path("content").get(0);
        assertThat(item.path("courseName").asText()).isEqualTo("SQL%_! 분석");
        assertThat(item.path("courseKey").asText()).isEqualTo(COURSE_KEY);
        assertThat(item.path("availableSeats").asInt()).isEqualTo(1);
        assertThat(item.path("occurredAt").asText()).isEqualTo(INBOX_OCCURRED_AT);
        assertThat(item.path("readAt").isNull()).isTrue();
        assertThat(state().path("activeInbox").asInt()).isEqualTo(64);
        assertThat(get("/api/v1/notifications/history").statusCode()).isEqualTo(200);
        assertThat(get(INBOX + "?page=-1").statusCode()).isEqualTo(400);
    }

    @Test
    @DisplayName("읽음·전체 읽음·삭제는 로그인한 소유자의 보이는 항목에만 반복 적용된다")
    void inboxMutationsAreScopedAndIdempotent() throws Exception {
        // given
        seedInbox(2);
        loginFixture("?other=true");
        long otherId = firstInboxItem("?read=UNREAD").path("id").asLong();
        loginFixture();
        JsonNode myItems = inbox("").path("content");
        long readId = myItems.get(0).path("id").asLong();
        long deleteId = myItems.get(1).path("id").asLong();

        // when & then
        mutateInbox("PATCH", "/" + otherId + "/read");
        mutateInbox("DELETE", "/" + otherId);
        mutateInbox("PATCH", "/" + readId + "/read");
        String readAt = firstInboxItem("?read=READ").path("readAt").asText();
        assertThat(readAt).isNotBlank();
        mutateInbox("PATCH", "/" + readId + "/read");
        assertThat(firstInboxItem("?read=READ").path("readAt").asText()).isEqualTo(readAt);
        assertThat(inbox("?read=UNREAD").path("totalElements").asInt()).isEqualTo(1);
        mutateInbox("PATCH", "/read-all");
        assertThat(inbox("?read=UNREAD").path("totalElements").asInt()).isZero();
        assertThat(inbox("?read=READ").path("totalElements").asInt()).isEqualTo(2);
        mutateInbox("DELETE", "/" + deleteId);
        mutateInbox("DELETE", "/" + deleteId);
        mutateInbox("PATCH", "/" + deleteId + "/read");
        assertThat(inbox("").path("totalElements").asInt()).isEqualTo(1);
        assertThat(firstInboxItem("").path("id").asLong()).isEqualTo(readId);

        loginFixture("?other=true");
        assertThat(inbox("?read=UNREAD").path("totalElements").asInt()).isEqualTo(2);
    }

    @Nested
    class InboxPublications {
        @Test
        @DisplayName("중요 공지만 알림함에 들어오고 공지 복원 뒤 같은 읽음 알림으로 확인한다")
        void importantAnnouncementPreservesItsInboxItemOnRestore() throws Exception {
            // given
            simulatedProviderCall = true;
            given(emailSender.supports(NotificationChannel.EMAIL)).willReturn(true);
            assertThat(post("/enable-email").statusCode()).isEqualTo(200);
            loginFixture("?admin=true");
            assertThat(apiPost(ADMIN_ANNOUNCEMENTS, Map.of("title", "일반 공지", "content", "내용")).statusCode())
                    .isEqualTo(200);
            HttpResponse<String> created = apiPost(ADMIN_ANNOUNCEMENTS,
                    Map.of("title", "중요 공지", "content", "중요 내용", "notify", true));
            assertThat(created.statusCode()).isEqualTo(200);
            assertThat(post("/dispatch-inbox").statusCode()).isEqualTo(200);
            verify(emailSender).sendDelivery(any(), eq("중요 공지"), anyString(), anyString(), eq("/notifications"));
            long announcementId = mapper.readTree(created.body()).path("id").asLong();
            loginFixture();
            JsonNode announcement = firstInboxItem("?type=ANNOUNCEMENT");
            long inboxId = announcement.path("id").asLong();
            assertThat(announcement.path("title").asText()).isEqualTo("중요 공지");
            assertThat(announcement.path("targetId").asLong()).isEqualTo(announcementId);
            assertThat(inbox("?type=ANNOUNCEMENT").path("totalElements").asInt()).isEqualTo(1);
            mutateInbox("PATCH", "/" + inboxId + "/read");

            // when
            loginFixture("?admin=true");
            assertThat(apiMutation("DELETE", ADMIN_ANNOUNCEMENTS + "/" + announcementId).statusCode()).isEqualTo(200);
            loginFixture();
            assertThat(firstInboxItem("?type=ANNOUNCEMENT").path("available").asBoolean()).isFalse();
            assertThat(get("/api/v1/announcements/" + announcementId).statusCode()).isEqualTo(404);
            loginFixture("?admin=true");
            assertThat(apiMutation("PATCH", ADMIN_ANNOUNCEMENTS + "/" + announcementId + "/restore").statusCode())
                    .isEqualTo(200);

            // then
            loginFixture();
            JsonNode restored = firstInboxItem("?type=ANNOUNCEMENT&read=READ");
            assertThat(restored.path("id").asLong()).isEqualTo(inboxId);
            assertThat(restored.path("available").asBoolean()).isTrue();
            assertThat(get("/api/v1/announcements/" + announcementId).statusCode()).isEqualTo(200);
            assertThat(inbox("?type=ANNOUNCEMENT").path("totalElements").asInt()).isEqualTo(1);
        }

        @Test
        @DisplayName("문의 답변과 최종 발송 실패는 작성자에게만 보이고 답변 삭제는 도착 알림만 숨긴다")
        void replyInboxIsPrivateAndFollowsReplyDeletion() throws Exception {
            // given
            simulatedProviderCall = true;
            given(emailSender.supports(NotificationChannel.EMAIL)).willReturn(true);
            doThrow(new NotificationProviderException(ErrorCode.EMAIL_SEND_ERROR, false,
                    "isolated-permanent-failure", 400, null))
                    .when(emailSender).sendDelivery(any(), eq(REPLY_NOTIFICATION_TITLE), anyString(), anyString(),
                            eq("/notifications"));
            assertThat(post("/enable-email").statusCode()).isEqualTo(200);
            loginFixture();
            HttpResponse<String> feedbacks = get("/api/v1/feedbacks/me");
            assertThat(feedbacks.statusCode()).isEqualTo(200);
            long feedbackId = mapper.readTree(feedbacks.body()).path("content").get(0).path("id").asLong();

            // when
            loginFixture("?admin=true");
            HttpResponse<String> reply = apiPost(ADMIN_FEEDBACKS + "/" + feedbackId + "/reply",
                    Map.of("content", "작성자에게만 보이는 답변"));
            assertThat(reply.statusCode()).isEqualTo(201);
            assertThat(post("/dispatch-inbox").statusCode()).isEqualTo(200);
            verify(emailSender).sendDelivery(any(), eq(REPLY_NOTIFICATION_TITLE), anyString(), anyString(),
                    eq("/notifications"));
            assertThat(post("/dispatch-inbox").statusCode()).isEqualTo(200);
            verify(emailSender, times(1)).sendDelivery(any(), anyString(), anyString(), anyString(), eq("/notifications"));
            long replyId = mapper.readTree(reply.body()).asLong();
            assertThat(inbox("?type=FEEDBACK_REPLY").path("totalElements").asInt()).isZero();
            loginFixture();
            JsonNode notice = firstInboxItem("?type=FEEDBACK_REPLY&read=UNREAD");
            assertThat(notice.path("targetId").asLong()).isEqualTo(feedbackId);
            assertThat(notice.path("message").asText()).doesNotContain("작성자에게만 보이는 답변");
            assertThat(firstInboxItem("?type=DELIVERY_FAILED").path("title").asText())
                    .isEqualTo("이메일 알림 발송 실패");
            loginFixture("?admin=true");
            assertThat(apiMutation("DELETE", ADMIN_FEEDBACKS + "/reply/" + replyId).statusCode()).isEqualTo(204);

            // then
            loginFixture();
            assertThat(inbox("?type=FEEDBACK_REPLY").path("totalElements").asInt()).isZero();
            assertThat(inbox("?type=DELIVERY_FAILED").path("totalElements").asInt()).isEqualTo(1);
        }
    }

    @Test
    @DisplayName("기존 두 알림 URL은 실제 보안·CSRF 처리를 거쳐 이동한 서비스에 도달한다")
    void bothNotificationUrlsReachTheMovedUseCaseWithRealSecurityAndCsrf() throws Exception {
        // given
        loginFixture();

        // when & then
        for (String path : List.of("/api/v1/notifications/test", "/api/v1/users/notifications/test")) {
            HttpResponse<String> response = apiPost(path, Map.of());
            assertThat(response.statusCode()).isEqualTo(400);
            assertThat(mapper.readTree(response.body()).path("code").asText())
                    .isEqualTo(ErrorCode.INVALID_INPUT.getCode());
        }
        verify(notificationUseCase, times(2)).sendTestNotification();
    }

    @Test
    @DisplayName("발송 제공자 실패는 커밋된 강의를 유지하고 재시도를 예약한다")
    void providerFailureDoesNotRollbackTheCommittedCatalogAndSchedulesRetry() throws Exception {
        // given
        simulatedProviderCall = true;
        given(emailSender.supports(NotificationChannel.EMAIL)).willReturn(true);
        doThrow(new NotificationProviderException(ErrorCode.EMAIL_SEND_ERROR, true,
                "isolated-provider-failure", 503, null))
                .when(emailSender).sendDelivery(any(), anyString(), anyString(), anyString(), eq("/"));

        // when & then
        assertThat(post("/enable-email").statusCode()).isEqualTo(200);
        assertThat(post("/crawl?mode=commit").statusCode()).isEqualTo(200);

        HttpResponse<String> response = post("/dispatch");

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(mapper.readTree(response.body()).path("claimed").asInt()).isEqualTo(1);
        verify(emailSender).sendDelivery(any(), anyString(), anyString(), anyString(), eq("/"));
        JsonNode state = state();
        assertThat(state.path("current").asInt()).isEqualTo(59);
        assertThat(state.path("history").asInt()).isEqualTo(1);
        assertThat(state.path("success").asInt()).isEqualTo(1);
        assertThat(state.path("outbox").asInt()).isEqualTo(1);
        assertThat(state.path("retryDeliveries").asInt()).isEqualTo(1);
    }

    @Test
    @DisplayName("관리자 재전송과 감사 기록은 함께 커밋된다")
    void administratorReplayAndAuditCommitTogether() throws Exception {
        // given
        long deliveryId = seedDeadLetterAndLoginAdmin();

        // when
        HttpResponse<String> response = apiPost("/api/v1/admin/notification-deliveries/" + deliveryId
                + "/replay", Map.of());

        // then
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(state().path("pendingDeliveries").asInt()).isEqualTo(1);
        assertThat(state().path("audit").asInt()).isEqualTo(1);
    }

    @Test
    @DisplayName("감사 기록 저장 실패는 관리자 재전송도 롤백한다")
    void auditFailureRollsBackAdministratorReplay() throws Exception {
        // given
        long deliveryId = seedDeadLetterAndLoginAdmin();
        doThrow(new IllegalStateException("isolated audit failure")).when(audits).save(any());

        // when
        HttpResponse<String> response = apiPost("/api/v1/admin/notification-deliveries/" + deliveryId
                + "/replay", Map.of());

        // then
        assertThat(response.statusCode()).isEqualTo(500);
        JsonNode state = state();
        assertThat(state.path("pendingDeliveries").asInt()).isZero();
        assertThat(state.path("deadLetters").asInt()).isEqualTo(1);
        assertThat(state.path("audit").asInt()).isZero();
    }

    private long seedDeadLetterAndLoginAdmin() throws Exception {
        HttpResponse<String> seeded = post("/seed-dead-letter");
        assertThat(seeded.statusCode()).isEqualTo(200);
        assertThat(post("/login?admin=true").statusCode()).isEqualTo(200);
        csrf = mapper.readTree(get("/api/auth/csrf").body()).path("token").asText();
        return mapper.readTree(seeded.body()).path("deliveryId").asLong();
    }

    private void loginFixture() throws Exception {
        loginFixture("");
    }

    private void loginFixture(String query) throws Exception {
        csrf = mapper.readTree(get("/api/auth/csrf").body()).path("token").asText();
        assertThat(post("/login" + query).statusCode()).isEqualTo(200);
        csrf = mapper.readTree(get("/api/auth/csrf").body()).path("token").asText();
    }

    private void seedInbox(int events) throws Exception {
        assertThat(post("/seed-inbox?events=" + events).statusCode()).isEqualTo(200);
        for (int pass = 0; pass < 2; pass++) {
            HttpResponse<String> dispatch = post("/dispatch");
            assertThat(dispatch.statusCode()).isEqualTo(200);
            assertThat(mapper.readTree(dispatch.body()).path("claimed").asInt()).isZero();
        }
    }

    private JsonNode inbox(String query) throws Exception {
        HttpResponse<String> response = get(INBOX + query);
        assertThat(response.statusCode()).isEqualTo(200);
        return mapper.readTree(response.body());
    }

    private JsonNode firstInboxItem(String query) throws Exception {
        return inbox(query).path("content").get(0);
    }

    private void mutateInbox(String method, String path) throws Exception {
        assertThat(apiMutation(method, INBOX + path).statusCode()).isEqualTo(204);
    }

    private HttpResponse<String> apiMutation(String method, String path) throws Exception {
        return browser.send(HttpRequest.newBuilder(uri(path))
                .timeout(Duration.ofSeconds(15)).header("X-CSRF-TOKEN", csrf)
                .method(method, HttpRequest.BodyPublishers.noBody()).build(), HttpResponse.BodyHandlers.ofString());
    }

    private JsonNode search(Map<String, Object> criteria, int page) throws Exception {
        HttpResponse<String> response = apiPost("/api/v1/courses/search?page=" + page, criteria);
        assertThat(response.statusCode()).isEqualTo(200);
        return mapper.readTree(response.body());
    }

    private HttpResponse<String> apiPost(String path, Map<String, Object> body) throws Exception {
        return browser.send(HttpRequest.newBuilder(uri(path)).timeout(Duration.ofSeconds(20))
                .header("X-CSRF-TOKEN", csrf).header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(body))).build(),
                HttpResponse.BodyHandlers.ofString());
    }

    private void assertCatalogRolledBack(JsonNode state) {
        assertThat(state.path("current").asInt()).isEqualTo(60);
        assertThat(state.path("history").asInt()).isZero();
        assertThat(state.path("outbox").asInt()).isZero();
        assertThat(state.path("success").asInt()).isZero();
    }

    private void assertWithdrawalRolledBack(JsonNode state, int activeSubscriptions) {
        assertThat(state.path("withdrawn").asInt()).isZero();
        assertThat(state.path("activeSubscriptions").asInt()).isEqualTo(activeSubscriptions);
        assertThat(state.path("activeFeedback").asInt()).isEqualTo(1);
        assertThat(state.path("devices").asInt()).isEqualTo(1);
    }

    private JsonNode state() throws Exception {
        HttpResponse<String> response = get(PREFIX + "/state");
        assertThat(response.statusCode()).isEqualTo(200);
        return mapper.readTree(response.body());
    }

    private HttpResponse<String> get(String path) throws Exception {
        return browser.send(HttpRequest.newBuilder(uri(path)).timeout(Duration.ofSeconds(15)).GET().build(),
                HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> post(String path) throws Exception {
        return browser.send(HttpRequest.newBuilder(uri(PREFIX + path)).timeout(Duration.ofSeconds(20))
                .header("X-CSRF-TOKEN", csrf).POST(HttpRequest.BodyPublishers.noBody()).build(),
                HttpResponse.BodyHandlers.ofString());
    }

    private URI uri(String path) {
        return URI.create("http://127.0.0.1:" + port + path);
    }

    @TestConfiguration
    @Import(FixtureController.class)
    static class Fixture {

        @Bean
        static BeanFactoryPostProcessor requireIsolatedResources(Environment environment) {
            return beanFactory -> {
                String url = environment.getRequiredProperty("spring.datasource.url");
                String redisHost = environment.getRequiredProperty("spring.data.redis.host");
                if (!url.startsWith("jdbc:h2:mem:modulith-e2e")
                        || !List.of("localhost", "127.0.0.1").contains(redisHost)) {
                    throw new IllegalStateException("E2E requires in-memory H2 and loopback Redis");
                }
            };
        }
    }

    @TestComponent
    @RestController
    @RequestMapping(PREFIX)
    @RequiredArgsConstructor
    static class FixtureController {

        private final DataSource dataSource;
        private final JdbcTemplate jdbc;
        private final CourseRepository courses;
        private final UserRepository users;
        private final UserDeviceRepository devices;
        private final SubscriptionRepository subscriptions;
        private final WishlistRepository wishlists;
        private final SeatNotificationOutboxRepository outboxes;
        private final SeatNotificationDeliveryRepository deliveries;
        private final SeatNotificationOutboxProcessor processor;
        private final SeatNotificationDeliveryStateService deliveryStateService;
        private final InboxChannelDeliveryWorker inboxDeliveryWorker;
        private final FeedbackRepository feedback;
        private final CourseCrawlCompletionService completion;
        private final UserService userService;
        private final ApplicationEventPublisher events;
        private final PlatformTransactionManager transactions;

        @PostMapping("/reset")
        void reset() throws Exception {
            try (var connection = dataSource.getConnection()) {
                if (!connection.getMetaData().getURL().startsWith("jdbc:h2:mem:modulith-e2e")) {
                    throw new IllegalStateException("Fixture requires its isolated in-memory database");
                }
            }
            for (String table : List.of("inbox_channel_deliveries", "seat_inbox_items", "seat_notification_deliveries",
                    "admin_action_logs", "seat_notification_outbox",
                    "course_seat_histories", "crawler_status",
                    "subscriptions", "wishlists", "user_devices", "feedback_replies", "feedbacks",
                    "announcements", "users", "courses")) {
                jdbc.update("DELETE FROM " + table);
            }
            courses.saveAndFlush(Course.builder().courseKey(COURSE_KEY).name("격리 강의").professor("격리 교수")
                    .subjectCode("E2E101").classNumber("01").capacity(60).current(60)
                    .academicYear("2026").semester("U211600010").build());
            User user = users.saveAndFlush(User.builder().name("격리 사용자").email(EMAIL).role(Role.USER).build());
            devices.saveAndFlush(UserDevice.builder().userId(user.getId()).token("https://example.invalid/e2e")
                    .p256dh("isolated-key").auth("isolated-auth").build());
            subscriptions.saveAndFlush(Subscription.builder().userId(user.getId()).courseKey(COURSE_KEY)
                    .isActive(true).build());
            feedback.saveAndFlush(Feedback.builder().userId(user.getId()).type(FeedbackType.BUG)
                    .title("격리 문의").content("격리 본문").build());
        }

        @PostMapping("/login")
        void login(HttpServletRequest request, HttpServletResponse response,
                   @RequestParam(defaultValue = "false") boolean admin,
                   @RequestParam(defaultValue = "false") boolean other) {
            User user = other ? users.findByEmail(OTHER_EMAIL).orElseThrow() : requireLoginUser(admin);
            SecurityContext context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(new SessionPrincipal(user.getId(), user.getEmail()).authentication(user.getRole().getKey()));
            new HttpSessionSecurityContextRepository().saveContext(context, request, response);
        }

        private User requireLoginUser(boolean admin) {
            if (admin) {
                return users.findByEmail("admin@example.invalid").orElseGet(() ->
                        users.saveAndFlush(User.builder().name("격리 관리자")
                                .email("admin@example.invalid").role(Role.ADMIN).build()));
            }
            return users.findByEmail(EMAIL).orElseThrow();
        }

        @PostMapping("/enable-email")
        void enableEmail() {
            User user = users.findByEmail(EMAIL).orElseThrow();
            user.updateSettings(EMAIL, true, false, false);
            users.saveAndFlush(user);
        }

        @PostMapping("/dispatch")
        Map<String, Integer> dispatch() {
            processor.materializePendingOutboxes();
            var claims = deliveryStateService.claimReadyDeliveries();
            claims.forEach(processor::processDelivery);
            return Map.of("claimed", claims.size());
        }

        @PostMapping("/dispatch-inbox")
        void dispatchInbox() {
            inboxDeliveryWorker.processReadyDeliveries();
        }

        @PostMapping("/seed-inbox")
        void seedInbox(@RequestParam int events) {
            User other = users.saveAndFlush(User.builder().name("다른 알림함 사용자")
                    .email(OTHER_EMAIL).role(Role.USER).build());
            subscriptions.saveAndFlush(Subscription.builder().userId(other.getId()).courseKey(COURSE_KEY)
                    .isActive(true).build());
            for (int event = 0; event < events; event++) {
                String name = event == 0 ? "SQL%_! 분석" : "SQLXYZ! 분석 " + event;
                jdbc.update("""
                        insert into seat_notification_outbox
                            (course_key, course_name, previous_seats, current_seats, status, version, created_at)
                        values (?, ?, 0, 1, 'PENDING', 0, ?)
                        """, COURSE_KEY, name, LocalDateTime.parse(INBOX_OCCURRED_AT));
            }
        }

        @PostMapping("/seed-dead-letter")
        Map<String, Long> seedDeadLetter() {
            SeatNotificationOutbox event = SeatNotificationOutbox.from(
                    new SeatOpenedEvent(COURSE_KEY, "격리 강의", "격리 교수", 0, 1));
            event.markDeadLettered();
            outboxes.saveAndFlush(event);
            SeatNotificationDelivery delivery = SeatNotificationDelivery.builder().outbox(event)
                    .userId(users.findByEmail(EMAIL).orElseThrow().getId()).channel(NotificationChannel.EMAIL).build();
            delivery.markFailure("ISOLATED_FAILURE", 1, LocalDateTime.now(), LocalDateTime.now());
            return Map.of("deliveryId", deliveries.saveAndFlush(delivery).getId());
        }

        @PostMapping("/seed-search")
        void seedSearch() {
            Course second = courses.saveAndFlush(searchCourse("modulith-course-b", 20));
            Course third = courses.saveAndFlush(searchCourse("modulith-course-c", 30));
            for (int index = 0; index < 33; index++) {
                courses.saveAndFlush(searchCourse("modulith-fill-%03d".formatted(index), 0));
            }
            User primary = users.findByEmail(EMAIL).orElseThrow();
            User other = users.saveAndFlush(User.builder().name("격리 사용자 2")
                    .email("other2@example.invalid").role(Role.USER).build());
            User last = users.saveAndFlush(User.builder().name("격리 사용자 3")
                    .email("other3@example.invalid").role(Role.USER).build());
            wishlists.saveAndFlush(Wishlist.builder().userId(primary.getId()).courseKey(second.getCourseKey()).build());
            wishlists.saveAndFlush(Wishlist.builder().userId(other.getId()).courseKey(second.getCourseKey()).build());
            wishlists.saveAndFlush(Wishlist.builder().userId(other.getId()).courseKey(third.getCourseKey()).build());
            wishlists.saveAndFlush(Wishlist.builder().userId(last.getId()).courseKey(third.getCourseKey()).build());
        }

        private Course searchCourse(String key, int current) {
            return Course.builder().courseKey(key).name("격리 검색 강의").professor("격리 교수")
                    .subjectCode("E2E101").classNumber("01").capacity(60).current(current)
                    .academicYear("2026").semester("U211600010").build();
        }

        @PostMapping("/crawl")
        void crawl(@RequestParam String mode) {
            List<ParsedCourse> parsed = new ArrayList<>();
            if ("chunk-failure".equals(mode)) {
                for (int index = 0; index < 100; index++) {
                    parsed.add(parsed("isolated-new-" + index));
                }
            }
            parsed.add(parsed(COURSE_KEY));
            if ("rollback".equals(mode)) {
                new TransactionTemplate(transactions).executeWithoutResult(status -> {
                    completion.complete(parsed);
                    throw new IllegalStateException("forced isolated caller rollback");
                });
                return;
            }
            completion.complete(parsed);
        }

        @PostMapping("/withdraw")
        void withdraw(@RequestParam boolean rollback) {
            SecurityContext original = SecurityContextHolder.getContext();
            SecurityContext context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(UsernamePasswordAuthenticationToken.authenticated(EMAIL, "unused", List.of()));
            SecurityContextHolder.setContext(context);
            try {
                withdrawUser(rollback);
            } finally {
                SecurityContextHolder.setContext(original);
            }
        }

        private void withdrawUser(boolean rollback) {
            if (rollback) {
                new TransactionTemplate(transactions).executeWithoutResult(status -> {
                    userService.withdraw();
                    throw new IllegalStateException("forced isolated withdrawal rollback");
                });
                return;
            }
            userService.withdraw();
        }

        @PostMapping("/outside")
        void outside(@RequestParam String event) {
            if ("seat".equals(event)) {
                events.publishEvent(new SeatOpenedEvent(COURSE_KEY, "격리 강의", "격리 교수", 0, 1));
                return;
            }
            events.publishEvent(new UserWithdrawnEvent(users.findByEmail(EMAIL).orElseThrow().getId()));
        }

        @GetMapping("/state")
        Map<String, Object> state() {
            return Map.ofEntries(
                    Map.entry("courses", count("SELECT COUNT(*) FROM courses")),
                    Map.entry("current", count("SELECT current FROM courses WHERE course_key='modulith-e2e-course'")),
                    Map.entry("history", count("SELECT COUNT(*) FROM course_seat_histories")),
                    Map.entry("outbox", count("SELECT COUNT(*) FROM seat_notification_outbox")),
                    Map.entry("success", count("SELECT COUNT(*) FROM crawler_status WHERE last_success_at IS NOT NULL")),
                    Map.entry("withdrawn", count("SELECT COUNT(*) FROM users WHERE deleted_at IS NOT NULL")),
                    Map.entry("activeSubscriptions", count("SELECT COUNT(*) FROM subscriptions WHERE is_active=TRUE")),
                    Map.entry("activeFeedback", count("SELECT COUNT(*) FROM feedbacks WHERE deleted_at IS NULL")),
                    Map.entry("devices", count("SELECT COUNT(*) FROM user_devices")),
                    Map.entry("activeInbox", count("SELECT COUNT(*) FROM seat_inbox_items WHERE deleted_at IS NULL")),
                    Map.entry("retryDeliveries", count("SELECT COUNT(*) FROM seat_notification_deliveries "
                            + "WHERE status='PENDING' AND attempts=1")),
                    Map.entry("pendingDeliveries", count("SELECT COUNT(*) FROM seat_notification_deliveries "
                            + "WHERE status='PENDING'")),
                    Map.entry("deadLetters", count("SELECT COUNT(*) FROM seat_notification_deliveries WHERE status='DLQ'")),
                    Map.entry("audit", count("SELECT COUNT(*) FROM admin_action_logs")));
        }

        private long count(String query) {
            return jdbc.queryForObject(query, Long.class);
        }

        private ParsedCourse parsed(String key) {
            return new ParsedCourse(key, "E2E101", null, "격리 강의", "01", "격리 교수", 60, 59,
                    null, "2026", "U211600010", null, null, null, null, null, null, null, null, null,
                    null, null, null, null, null, null, null, null, null, List.of());
        }
    }
}
