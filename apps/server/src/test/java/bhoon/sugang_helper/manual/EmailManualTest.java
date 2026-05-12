package bhoon.sugang_helper.manual;

import bhoon.sugang_helper.domain.notification.sender.EmailNotificationSender;
import bhoon.sugang_helper.domain.notification.sender.NotificationTarget;
import bhoon.sugang_helper.domain.user.service.EmailVerificationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * AWS SES를 통한 실제 이메일 발송을 수동으로 테스트하기 위한 클래스입니다.
 * 이 테스트는 실제 메일이 발송되므로 'manual' 태그를 사용하여 평소에는 실행되지 않도록 설정되어 있습니다.
 */
@Tag("manual")
@SpringBootTest
public class EmailManualTest {

    @Autowired
    private EmailVerificationService emailVerificationService;

    @Autowired
    private EmailNotificationSender emailNotificationSender;

    private static final String TEST_EMAIL = "leafloveu@naver.com";

    @Test
    @DisplayName("인증 코드 HTML 메일 발송 수동 테스트")
    void sendVerificationEmail() {
        System.out.println("SES 인증 메일 발송 테스트 시작: " + TEST_EMAIL);
        emailVerificationService.sendCode(9999L, TEST_EMAIL);
        System.out.println("SES 인증 메일 발송 요청 완료!");
    }

    @Test
    @DisplayName("여석 알림 HTML 메일 발송 수동 테스트")
    void sendNotificationEmail() {
        System.out.println("SES 알림 메일 발송 테스트 시작: " + TEST_EMAIL);
        String courseName = "운영체제";
        String professor = "홍길동";
        String title = String.format("[줍줍] %s (%s 교수) 여석 발생!", courseName, professor);
        String message = String.format("<strong>[%s (%s 교수)]</strong> 과목에 <strong>1명</strong>의 여석이 발생했습니다.", courseName, professor);
        
        emailNotificationSender.send(NotificationTarget.of(TEST_EMAIL), title, message);
        System.out.println("SES 알림 메일 발송 요청 완료!");
    }
}
