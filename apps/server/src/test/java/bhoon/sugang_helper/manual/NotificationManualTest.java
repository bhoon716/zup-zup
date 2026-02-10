package bhoon.sugang_helper.manual;

import bhoon.sugang_helper.domain.course.repository.CourseRepository;
import bhoon.sugang_helper.domain.course.service.CourseCrawlerService;
import bhoon.sugang_helper.domain.course.service.JbnuCourseApiClient;
import bhoon.sugang_helper.domain.subscription.entity.Subscription;
import bhoon.sugang_helper.domain.subscription.repository.SubscriptionRepository;
import bhoon.sugang_helper.domain.user.entity.Role;
import bhoon.sugang_helper.domain.user.entity.User;
import bhoon.sugang_helper.domain.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.mockito.BDDMockito.given;

@SpringBootTest
@Tag("notification")
class NotificationManualTest {

    @Autowired
    private CourseCrawlerService courseCrawlerService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private SubscriptionRepository subscriptionRepository;

    @MockitoBean
    private JbnuCourseApiClient jbnuCourseApiClient;

    @org.springframework.beans.factory.annotation.Value("${app.manual-test.email}")
    private String testEmail;

    @org.springframework.beans.factory.annotation.Value("${app.manual-test.discord-id}")
    private String testDiscordId;

    @Autowired
    private bhoon.sugang_helper.common.redis.RedisService redisService;

    @Test
    @DisplayName("[Manual] 빈자리 알림 전체 흐름 테스트 (Parser -> Notification)")
    void manualEndToEndNotificationTest() throws Exception {
        // 0. Redis Key 초기화 (Dedup 방지)
        String year = "2024";
        String semester = "10";
        String subjectCode = "TEST001";
        String classNumber = "01";
        String courseKey = String.format("%s:%s:%s:%s", year, semester, subjectCode, classNumber);
        redisService.deleteValues("ALERT:" + courseKey);
        System.out.println(">>> [ManualTest] Redis Dedup Key 초기화 완료: " + "ALERT:" + courseKey);

        // 1. 테스트용 유저 및 구독 설정
        User user = userRepository.save(User.builder()
                .email(testEmail)
                .name("Manual Tester")
                .role(Role.USER)
                .emailEnabled(true)
                .discordEnabled(true)
                .discordId(testDiscordId)
                .webPushEnabled(true)
                .build());

        // 2. 초기 상태: 정원이 꽉 찬 상태의 과목 데이터가 있다고 가정
        bhoon.sugang_helper.domain.course.entity.Course existingCourse = bhoon.sugang_helper.domain.course.entity.Course
                .builder()
                .courseKey(courseKey)
                .subjectCode(subjectCode)
                .classNumber(classNumber)
                .name("수동 테스트 과목")
                .capacity(50)
                .current(50)
                .academicYear(year)
                .semester(semester)
                .build();
        courseRepository.save(existingCourse);

        // 구독 설정
        subscriptionRepository.save(Subscription.builder()
                .userId(user.getId())
                .courseKey(courseKey)
                .isActive(true)
                .build());

        // 3. Mock XML 응답 설정 (여석 발생 상황)
        String mockXmlResponse = """
                <Root>
                    <Dataset id="GRD_COUR001">
                        <Rows>
                            <Row>
                                <Col id="SBJTCD">TEST001</Col>
                                <Col id="CLSS">01</Col>
                                <Col id="YY">2024</Col>
                                <Col id="SHTM">10</Col>
                                <Col id="SBJTNM">수동 테스트 과목</Col>
                                <Col id="RPSTPROFNM">테스트 교수</Col>
                                <Col id="LMTRCNT">50</Col>     <!-- 정원 -->
                                <Col id="TLSNRCNT">49</Col>    <!-- 수강인원 (1자리 빔!) -->
                                <Col id="TLSNOBJFGNM">전학년</Col>
                                <Col id="CPTNFGNM">전필</Col>
                                <Col id="SUSTCDNM">컴퓨터공학부</Col>
                                <Col id="SCORTRETFGNM">학점</Col>
                                <Col id="LTLANGFGNM">한국어</Col>
                                <Col id="DAYTMCTNT">월1,수2</Col>
                                <Col id="PNT">3</Col>
                                <Col id="TM">3</Col>
                                <Col id="VLDFGNM">유효</Col>
                                <Col id="OPENLECTFGNM">개설</Col>
                                <Col id="VILROOMNOCTNT">7-301</Col>
                                <Col id="SUBPLANYN">Y</Col>
                                <Col id="PUBCYN">Y</Col>
                                <Col id="NOPUBCRESNNM"></Col>
                            </Row>
                        </Rows>
                    </Dataset>
                </Root>
                """;

        given(jbnuCourseApiClient.fetchCourseDataXml()).willReturn(mockXmlResponse);

        // 4. 크롤러 실행
        System.out.println(">>> [ManualTest] 크롤링 시작: 여석 발생 시나리오");
        courseCrawlerService.crawlAndSaveCourses();
        System.out.println(">>> [ManualTest] 크롤링 종료");

        System.out.println(">>> [ManualTest] 비동기 알림 발송 대기 중 (5초)...");
        Thread.sleep(5000);
        System.out.println(">>> [ManualTest] 테스트 종료");
    }
}
