package bhoon.sugang_helper.domain.notification.sender;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@Tag("manual")
@SpringBootTest
public class EmailManualTest {

    @Autowired
    private EmailNotificationSender emailNotificationSender;

    @Test
    @DisplayName("Simulate seat opening alert email")
    void sendManualSeatOpeningAlert() {
        String recipient = "leafloveu@naver.com";
        String courseName = "운영체제";
        String courseKey = "10001-01";
        int currentSeats = 1;

        String title = String.format("[SugangHelper] 빈자리 알림: %s", courseName);
        String message = String.format("강의명: %s\n과목코드: %s\n현재 여석이 발생했습니다! (%d명)",
                courseName, courseKey, currentSeats);

        emailNotificationSender.send(recipient, title, message);
    }
}
